package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaEvidence;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
public class SubmitAppealService {
    public static final String DM_STORE_USER_ID = "sscs";

    private final CcdService ccdService;
    private final SscsPdfService sscsPdfService;
    private final RoboticsService roboticsService;
    private final AirLookupService airLookupService;
    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final IdamService idamService;
    private final EvidenceManagementService evidenceManagementService;

    @Autowired
    SubmitAppealService(CcdService ccdService,
                        SscsPdfService sscsPdfService,
                        RoboticsService roboticsService,
                        AirLookupService airLookupService,
                        RegionalProcessingCenterService regionalProcessingCenterService,
                        IdamService idamService,
                        EvidenceManagementService evidenceManagementService) {

        this.ccdService = ccdService;
        this.sscsPdfService = sscsPdfService;
        this.roboticsService = roboticsService;
        this.airLookupService = airLookupService;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.idamService = idamService;
        this.evidenceManagementService = evidenceManagementService;
    }

    public void submitAppeal(SyaCaseWrapper appeal) {

        String firstHalfOfPostcode = regionalProcessingCenterService
                .getFirstHalfOfPostcode(appeal.getContactDetails().getPostCode());
        SscsCaseData caseData = prepareCaseForCcd(appeal, firstHalfOfPostcode);
        IdamTokens idamTokens = idamService.getIdamTokens();
        SscsCaseDetails caseDetails = createCaseInCcd(caseData, idamTokens);
        postCreateCaseInCcdProcess(appeal, firstHalfOfPostcode, caseData, idamTokens, caseDetails);

    }

    private void postCreateCaseInCcdProcess(SyaCaseWrapper appeal, String firstHalfOfPostcode, SscsCaseData caseData,
                                            IdamTokens idamTokens, SscsCaseDetails caseDetails) {
        if (null != caseDetails) {
            byte[] pdf = sscsPdfService.generateAndSendPdf(caseData, caseDetails.getId(), idamTokens);
            Map<String, byte[]> additionalEvidence = downloadEvidence(appeal);
            JSONObject roboticsJson = roboticsService
                    .sendCaseToRobotics(caseData, caseDetails.getId(), firstHalfOfPostcode, pdf, additionalEvidence);

            attachRoboticJsonToCaseInCcdHandled(caseData, idamTokens, caseDetails, roboticsJson);
        }
    }

    private void attachRoboticJsonToCaseInCcdHandled(SscsCaseData caseData, IdamTokens idamTokens,
                                                     SscsCaseDetails caseDetails, JSONObject roboticsJson) {
        try {
            roboticsService.attachRoboticsJsonToCaseInCcd(roboticsJson, caseData, idamTokens, caseDetails);
        } catch (Exception e) {
            log.info("Failed to update ccd case with Robotics JSON but carrying on [" + caseDetails.getId() + "] ["
                    + caseData.getCaseReference() + "]", e);
        }
    }

    private SscsCaseData prepareCaseForCcd(SyaCaseWrapper appeal, String postcode) {
        String region = airLookupService.lookupRegionalCentre(postcode);
        RegionalProcessingCenter rpc = regionalProcessingCenterService.getByName(region);

        if (rpc == null) {
            return transformAppealToCaseData(appeal);
        } else {
            return transformAppealToCaseData(appeal, rpc.getName(), rpc);
        }
    }

    String getFirstHalfOfPostcode(String postcode) {
        if (postcode != null && postcode.length() > 3) {
            return postcode.substring(0, postcode.length() - 3).trim();
        }
        return "";
    }

    private SscsCaseDetails createCaseInCcd(SscsCaseData caseData, IdamTokens idamTokens) {
        SscsCaseDetails caseDetails = null;
        try {
            caseDetails = ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(caseData, idamTokens);
            if (caseDetails == null) {
                caseDetails = ccdService.createCase(caseData, idamTokens);
                log.info("Case {} successfully created in CCD for Nino - {} and benefit type {}",
                        caseDetails.getId(), caseData.getGeneratedNino(),
                        caseData.getAppeal().getBenefitType().getCode());
                return caseDetails;
            } else {
                log.info("Duplicate case {} found for Nino {} and benefit type {}. "
                                + "No need to continue with post create case processing.",
                        caseDetails.getId(), caseData.getGeneratedNino(),
                        caseData.getAppeal().getBenefitType().getCode());
                return null;
            }
        } catch (Exception e) {
            throw new CcdException(
                    String.format("Error found in the creating case process for case with Id - %s"
                                    + " and Nino - %s and Benefit type - %s",
                            caseDetails != null ? caseDetails.getId() : "", caseData.getGeneratedNino(),
                            caseData.getAppeal().getBenefitType().getCode()), e);
        }
    }

    private SscsCaseData transformAppealToCaseData(SyaCaseWrapper appeal) {
        return convertSyaToCcdCaseData(appeal);
    }

    SscsCaseData transformAppealToCaseData(SyaCaseWrapper appeal, String region, RegionalProcessingCenter rpc) {
        return convertSyaToCcdCaseData(appeal, region, rpc);
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
