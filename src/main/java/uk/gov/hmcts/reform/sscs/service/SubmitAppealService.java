package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData;

import java.net.URI;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaEvidence;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.model.SaveCaseResult;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;
import uk.gov.hmcts.reform.sscs.service.converter.ConvertAintoBService;

@Service
@Slf4j
public class SubmitAppealService {
    public static final String DM_STORE_USER_ID = "sscs";

    private final CcdService ccdService;
    private final CitizenCcdService citizenCcdService;
    private final SscsPdfService sscsPdfService;
    private final RoboticsService roboticsService;
    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final IdamService idamService;
    private final EvidenceManagementService evidenceManagementService;
    private final ConvertAintoBService convertAintoBService;

    @Value("${feature.send_to_dwp}")
    private Boolean sendToDwpFeature;

    @Autowired
    SubmitAppealService(CcdService ccdService,
                        CitizenCcdService citizenCcdService,
                        SscsPdfService sscsPdfService,
                        RoboticsService roboticsService,
                        RegionalProcessingCenterService regionalProcessingCenterService,
                        IdamService idamService,
                        EvidenceManagementService evidenceManagementService,
                        ConvertAintoBService convertAintoBService) {

        this.ccdService = ccdService;
        this.citizenCcdService = citizenCcdService;
        this.sscsPdfService = sscsPdfService;
        this.roboticsService = roboticsService;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.idamService = idamService;
        this.evidenceManagementService = evidenceManagementService;
        this.convertAintoBService = convertAintoBService;
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

    public SaveCaseResult submitDraftAppeal(String oauth2Token, SyaCaseWrapper appeal) {
        appeal.setCaseType("draft");
        return saveDraftCaseInCcd(convertSyaToCcdCaseData(appeal), getUserTokens(oauth2Token));
    }

    @SuppressWarnings("unchecked")
    public Optional<SessionDraft> getDraftAppeal(String oauth2Token) {
        List<SscsCaseData> caseDetailsList = citizenCcdService.findCase(getUserTokens(oauth2Token));
        if (CollectionUtils.isNotEmpty(caseDetailsList)) {
            SessionDraft sessionDraft = (SessionDraft) convertAintoBService.convert(caseDetailsList.get(0));
            return Optional.of(sessionDraft);
        }
        return Optional.empty();
    }

    private IdamTokens getUserTokens(String oauth2Token) {
        return IdamTokens.builder()
            .idamOauth2Token(oauth2Token)
            .serviceAuthorization(idamService.generateServiceAuthorization())
            .userId(idamService.getUserId(oauth2Token))
            .build();
    }

    private void postCreateCaseInCcdProcess(SyaCaseWrapper appeal, String firstHalfOfPostcode, SscsCaseData caseData,
                                            IdamTokens idamTokens, SscsCaseDetails caseDetails, EventType event) {
        if (null != caseDetails) {
            byte[] pdf = sscsPdfService.generateAndSendPdf(caseData, caseDetails.getId(), idamTokens, "sscs1");
            Map<String, byte[]> additionalEvidence = downloadEvidence(appeal);
            if (event.equals(SYA_APPEAL_CREATED)) {
                roboticsService.sendCaseToRobotics(caseData, caseDetails.getId(), firstHalfOfPostcode, pdf,
                    additionalEvidence);

                if (sendToDwpFeature) {
                    log.info("About to update case with sentToDwp event for id {}", caseDetails.getId());
                    ccdService.updateCase(caseData, caseDetails.getId(), SENT_TO_DWP.getCcdType(), "Sent to DWP", "Case has been sent to the DWP by Robotics", idamTokens);
                    log.info("Case updated with sentToDwp event for id {}", caseDetails.getId());
                }
            }
        }
    }

    SscsCaseData prepareCaseForCcd(SyaCaseWrapper appeal, String postcode) {
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
                log.info("Case {} successfully created in CCD for benefit type {} with event {}",
                    caseDetails.getId(),
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
                        + " and Nino - %s and Benefit type - %s and exception: %s",
                    caseDetails != null ? caseDetails.getId() : "", caseData.getGeneratedNino(),
                    caseData.getAppeal().getBenefitType().getCode(), e.getMessage()), e);
        }
    }

    private SaveCaseResult saveDraftCaseInCcd(SscsCaseData caseData, IdamTokens idamTokens) {
        SaveCaseResult result = citizenCcdService.saveCase(caseData, idamTokens);
        log.info("Draft Case {} successfully {} in CCD", result.getCaseDetailsId(), result.getSaveCaseOperation().name());
        return result;
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
