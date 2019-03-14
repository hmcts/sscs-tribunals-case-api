package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData;

import java.net.URI;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
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
    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final IdamService idamService;
    private final EvidenceManagementService evidenceManagementService;

    @Autowired
    SubmitAppealService(CcdService ccdService,
                        SscsPdfService sscsPdfService,
                        RoboticsService roboticsService,
                        RegionalProcessingCenterService regionalProcessingCenterService,
                        IdamService idamService,
                        EvidenceManagementService evidenceManagementService) {

        this.ccdService = ccdService;
        this.sscsPdfService = sscsPdfService;
        this.roboticsService = roboticsService;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.idamService = idamService;
        this.evidenceManagementService = evidenceManagementService;
    }

    public Long submitAppeal(SyaCaseWrapper appeal) {
        String firstHalfOfPostcode = regionalProcessingCenterService
                .getFirstHalfOfPostcode(appeal.getContactDetails().getPostCode());
        SscsCaseData caseData = prepareCaseForCcd(appeal, firstHalfOfPostcode);

        EventType event = findEventType(caseData);
        IdamTokens idamTokens = idamService.getIdamTokens();
        SscsCaseDetails caseDetails = createCaseInCcd(caseData, event, idamTokens);
        postCreateCaseInCcdProcess(appeal, firstHalfOfPostcode, caseData, idamTokens, caseDetails, event);
        // in case of duplicate case the caseDetails will be null
        return (caseDetails != null) ? caseDetails.getId() : null;
    }

    public Long submitDraftAppeal(SyaCaseWrapper appeal) {
        SscsCaseData caseData = convertSyaToCcdCaseData(appeal);

        IdamTokens idamTokens = idamService.getIdamTokens();
        SscsCaseDetails caseDetails = createCaseInCcd(caseData, EventType.DRAFT, idamTokens);

        // in case of duplicate case the caseDetails will be null
        return (caseDetails != null) ? caseDetails.getId() : null;
    }

    private void postCreateCaseInCcdProcess(SyaCaseWrapper appeal, String firstHalfOfPostcode, SscsCaseData caseData,
                                            IdamTokens idamTokens, SscsCaseDetails caseDetails, EventType event) {
        if (null != caseDetails) {
            byte[] pdf = sscsPdfService.generateAndSendPdf(caseData, caseDetails.getId(), idamTokens);
            Map<String, byte[]> additionalEvidence = downloadEvidence(appeal);
            if (event.equals(SYA_APPEAL_CREATED)) {
                roboticsService.sendCaseToRobotics(caseData, caseDetails.getId(), firstHalfOfPostcode, pdf,
                        additionalEvidence);
            }
        }
    }

    protected SscsCaseData prepareCaseForCcd(SyaCaseWrapper appeal, String postcode) {
        RegionalProcessingCenter rpc = regionalProcessingCenterService.getByPostcode(postcode);

        if (rpc == null) {
            return convertSyaToCcdCaseData(appeal);
        } else {
            return convertSyaToCcdCaseData(appeal, rpc.getName(), rpc);
        }
    }

    String getFirstHalfOfPostcode(String postcode) {
        if (postcode != null && postcode.length() > 3) {
            return postcode.substring(0, postcode.length() - 3).trim();
        }
        return "";
    }

    private SscsCaseDetails createCaseInCcd(SscsCaseData caseData, EventType eventType, IdamTokens idamTokens) {
        SscsCaseDetails caseDetails = null;
        try {
            caseDetails = ccdService.findCcdCaseByNinoAndBenefitTypeAndMrnDate(caseData, idamTokens);
            if (caseDetails == null) {
                caseDetails = ccdService.createCase(caseData,
                        eventType.getCcdType(),
                        "SSCS - new case created",
                        "Created SSCS case from Submit Your Appeal online with event " + eventType.getCcdType(),
                        idamTokens);
                log.info("Case {} successfully created in CCD for Nino - {} and benefit type {} with event {}",
                        caseDetails.getId(), caseData.getGeneratedNino(),
                        caseData.getAppeal().getBenefitType().getCode(),
                        eventType);
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

    private EventType findEventType(SscsCaseData caseData) {
        if (caseData.getAppeal().getMrnDetails() != null && caseData.getAppeal().getMrnDetails().getMrnDate() != null) {
            LocalDate mrnDate = LocalDate.parse(caseData.getAppeal().getMrnDetails().getMrnDate());
            return mrnDate.plusMonths(13L).isBefore(LocalDate.now()) ? NON_COMPLIANT : SYA_APPEAL_CREATED;
        } else {
            return INCOMPLETE_APPLICATION_RECEIVED;
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
