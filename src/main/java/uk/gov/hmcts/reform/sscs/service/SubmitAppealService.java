package uk.gov.hmcts.reform.sscs.service;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaEvidence;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer;

@Service
@Slf4j
public class SubmitAppealService {
    public static final String DM_STORE_USER_ID = "sscs";

    private final AppealNumberGenerator appealNumberGenerator;
    private final SubmitYourAppealToCcdCaseDataDeserializer submitYourAppealToCcdCaseDataDeserializer;
    private final CcdService ccdService;
    private final SscsPdfService sscsPdfService;
    private final RoboticsService roboticsService;
    private final AirLookupService airLookupService;
    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final IdamService idamService;
    private final EvidenceManagementService evidenceManagementService;

    @Autowired
    SubmitAppealService(AppealNumberGenerator appealNumberGenerator,
                        SubmitYourAppealToCcdCaseDataDeserializer submitYourAppealToCcdCaseDataDeserializer,
                        CcdService ccdService,
                        SscsPdfService sscsPdfService,
                        RoboticsService roboticsService,
                        AirLookupService airLookupService,
                        RegionalProcessingCenterService regionalProcessingCenterService,
                        IdamService idamService,
                        EvidenceManagementService evidenceManagementService) {

        this.appealNumberGenerator = appealNumberGenerator;
        this.submitYourAppealToCcdCaseDataDeserializer = submitYourAppealToCcdCaseDataDeserializer;
        this.ccdService = ccdService;
        this.sscsPdfService = sscsPdfService;
        this.roboticsService = roboticsService;
        this.airLookupService = airLookupService;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.idamService = idamService;
        this.evidenceManagementService = evidenceManagementService;
    }

    public void submitAppeal(SyaCaseWrapper appeal) {
        String firstHalfOfPostcode = regionalProcessingCenterService.getFirstHalfOfPostcode(appeal.getContactDetails().getPostCode());

        SscsCaseData caseData = prepareCaseForCcd(appeal, firstHalfOfPostcode);

        IdamTokens idamTokens = idamService.getIdamTokens();

        SscsCaseDetails caseDetails = createCaseInCcd(caseData, idamTokens);

        byte[] pdf = sscsPdfService.generateAndSendPdf(caseData, caseDetails.getId(), idamTokens);

        Map<String, byte[]> additionalEvidence = downloadEvidence(appeal);

        roboticsService.sendCaseToRobotics(caseData, caseDetails.getId(), firstHalfOfPostcode, pdf, additionalEvidence);
    }

    private SscsCaseData prepareCaseForCcd(SyaCaseWrapper appeal, String postcode) {
        String region = airLookupService.lookupRegionalCentre(postcode);
        RegionalProcessingCenter rpc = regionalProcessingCenterService.getByName(region);
        
        return transformAppealToCaseData(appeal, rpc);
    }

    protected String getFirstHalfOfPostcode(String postcode) {
        if (postcode != null && postcode.length() > 3) {
            return postcode.substring(0, postcode.length() - 3).trim();
        }

        return "";
    }

    private SscsCaseDetails createCaseInCcd(SscsCaseData caseData, IdamTokens idamTokens) {
        SscsCaseDetails caseDetails;

        try {
            caseDetails = ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(caseData, idamTokens);
        } catch (Exception e) {
            throw new CcdException("Error found in the case creation or callback process for ccd case with"
                                    + " Nino - " + caseData.getGeneratedNino() 
                                    + " and Benefit type - " + caseData.getAppeal().getBenefitType().getCode());
        }

        if (caseDetails != null) {
            throw new CcdException("Duplicate case found for Nino " + caseData.getGeneratedNino() 
                                    + " and benefit type " + caseData.getAppeal().getBenefitType().getCode());
        }

        try {
            caseDetails = ccdService.createCase(caseData, idamTokens);
        } catch (Exception e) {
            throw new CcdException("Error found in the case creation or callback process for ccd case with"
                                    + " Nino - " + caseData.getGeneratedNino() 
                                    + " and Benefit type - " + caseData.getAppeal().getBenefitType().getCode());
        }

        log.info("Appeal successfully created in CCD for Nino - {} and benefit type {}",
                caseData.getGeneratedNino(), caseData.getAppeal().getBenefitType().getCode());

        return caseDetails;
    }

    protected SscsCaseData transformAppealToCaseData(SyaCaseWrapper appeal, RegionalProcessingCenter rpc) {
        SscsCaseData caseData;

        if (null == rpc) {
            caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(appeal);
        } else {
            caseData = submitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData(appeal, rpc.getName(), rpc);
        }
    
        return updateCaseData(caseData);
    }

    private SscsCaseData updateCaseData(SscsCaseData caseData) {
        try {
            Subscription subscription = caseData.getSubscriptions().getAppellantSubscription().toBuilder()
                    .tya(appealNumberGenerator.generateAppealNumber())
                    .build();

            caseData.setSubscriptions(caseData.getSubscriptions().toBuilder().appellantSubscription(subscription).build());

            if (null != caseData.getSubscriptions().getRepresentativeSubscription()) {
                Subscription representativeSubscriptionBuilder =
                        caseData.getSubscriptions().getRepresentativeSubscription().toBuilder()
                                .tya(appealNumberGenerator.generateAppealNumber())
                                .build();
                caseData.setSubscriptions(caseData.getSubscriptions().toBuilder()
                        .representativeSubscription(representativeSubscriptionBuilder).build());
            }

            return caseData;
        } catch (CcdException e) {
            log.error("Appeal number is not generated for Nino - {} and Benefit Type - {}",
                    caseData.getGeneratedNino(), caseData.getAppeal().getBenefitType().getCode(), e);
            return caseData;
        }
    }

    private Map<String, byte[]> downloadEvidence(SyaCaseWrapper appeal) {
        if (hasEvidence(appeal)) {
            Map<String, byte[]> map = new LinkedHashMap<>();
            for (SyaEvidence evidence : appeal.getReasonsForAppealing().getEvidences()) {
                map.put(evidence.getFileName(), downloadBinary(evidence));
            }
            return map;
        } else {
            return Collections.emptyMap();
        }
    }

    private boolean hasEvidence(SyaCaseWrapper appeal) {
        return CollectionUtils.isNotEmpty(appeal.getReasonsForAppealing().getEvidences());
    }

    private byte[] downloadBinary(SyaEvidence evidence) {

        return evidenceManagementService.download(URI.create(evidence.getUrl()), DM_STORE_USER_ID);
    }
}
