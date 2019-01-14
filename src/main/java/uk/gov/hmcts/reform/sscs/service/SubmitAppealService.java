package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaEvidence;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@Service
@Slf4j
public class SubmitAppealService {
    public static final String DM_STORE_USER_ID = "sscs";
    public static final String S2S_TOKEN = "oauth2Token";

    private final CcdService ccdService;
    private final SscsPdfService sscsPdfService;
    private final RoboticsService roboticsService;
    private final AirLookupService airLookupService;
    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final IdamService idamService;
    private final EvidenceManagementService evidenceManagementService;
    private final RoboticsJsonUploadService roboticsJsonUploadService;

    @Autowired
    SubmitAppealService(CcdService ccdService,
                        SscsPdfService sscsPdfService,
                        RoboticsService roboticsService,
                        AirLookupService airLookupService,
                        RegionalProcessingCenterService regionalProcessingCenterService,
                        IdamService idamService,
                        EvidenceManagementService evidenceManagementService,
                        RoboticsJsonUploadService roboticsJsonUploadService) {

        this.ccdService = ccdService;
        this.sscsPdfService = sscsPdfService;
        this.roboticsService = roboticsService;
        this.airLookupService = airLookupService;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.idamService = idamService;
        this.evidenceManagementService = evidenceManagementService;
        this.roboticsJsonUploadService = roboticsJsonUploadService;
    }

    public void submitAppeal(SyaCaseWrapper appeal) {
        String firstHalfOfPostcode =
                regionalProcessingCenterService.getFirstHalfOfPostcode(appeal.getContactDetails().getPostCode());

        SscsCaseData caseData = prepareCaseForCcd(appeal, firstHalfOfPostcode);

        IdamTokens idamTokens = idamService.getIdamTokens();

        SscsCaseDetails caseDetails = createCaseInCcd(caseData, idamTokens);

        byte[] pdf = sscsPdfService.generateAndSendPdf(caseData, caseDetails.getId(), idamTokens);

        Map<String, byte[]> additionalEvidence = downloadEvidence(appeal);

        log.info("Sending case {} to Robotics", caseDetails.getId());
        roboticsService.sendCaseToRobotics(caseData, caseDetails.getId(), firstHalfOfPostcode, pdf, additionalEvidence);

        if (caseDetails.getId() == null) {
            log.info("caseId is empty - skipping step to update CCD with Robotics JSON");
        } else {
            roboticsJsonUploadService
                    .updateCaseWithRoboticsJson(roboticsService.getRoboticsJson(), caseData, caseDetails, idamTokens);
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
        try {
            SscsCaseDetails caseDetails = ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(caseData, idamTokens);
            if (caseDetails == null) {
                caseDetails = ccdService.createCase(caseData, idamTokens);
                log.info("Appeal successfully created in CCD for Nino - {} and benefit type {}",
                        caseData.getGeneratedNino(), caseData.getAppeal().getBenefitType().getCode());
                return caseDetails;
            } else {
                log.info("Duplicate case found for Nino {} and benefit type {} so not creating in CCD", caseData.getGeneratedNino(), caseData.getAppeal().getBenefitType().getCode());
                return caseDetails;
            }
        } catch (Exception e) {
            log.error("Error found in the case creation or callback process for ccd case with "
                            + "Nino - {} and Benefit type - {} ",
                    caseData.getGeneratedNino(), caseData.getAppeal().getBenefitType().getCode(), e);
            return SscsCaseDetails.builder().build();
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
