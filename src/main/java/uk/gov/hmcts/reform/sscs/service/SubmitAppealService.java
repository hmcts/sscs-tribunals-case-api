package uk.gov.hmcts.reform.sscs.service;

import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.*;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.exception.CcdException;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.config.CitizenCcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
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
    private final RegionalProcessingCenterService regionalProcessingCenterService;
    private final IdamService idamService;
    private final ConvertAintoBService convertAintoBService;

    @Autowired
    SubmitAppealService(CcdService ccdService,
                        CitizenCcdService citizenCcdService,
                        SscsPdfService sscsPdfService,
                        RegionalProcessingCenterService regionalProcessingCenterService,
                        IdamService idamService,
                        ConvertAintoBService convertAintoBService) {

        this.ccdService = ccdService;
        this.citizenCcdService = citizenCcdService;
        this.sscsPdfService = sscsPdfService;
        this.regionalProcessingCenterService = regionalProcessingCenterService;
        this.idamService = idamService;
        this.convertAintoBService = convertAintoBService;
    }

    public Long submitAppeal(SyaCaseWrapper appeal, String userToken) {
        String firstHalfOfPostcode = regionalProcessingCenterService
            .getFirstHalfOfPostcode(appeal.getContactDetails().getPostCode());
        SscsCaseData caseData = prepareCaseForCcd(appeal, firstHalfOfPostcode);

        EventType event = findEventType(caseData);
        log.info("Event type is " + event.getCcdType());
        IdamTokens idamTokens = idamService.getIdamTokens();
        SscsCaseDetails caseDetails = createCaseInCcd(caseData, event, idamTokens);
        postCreateCaseInCcdProcess(caseData, idamTokens, caseDetails, event, userToken);
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

    private void postCreateCaseInCcdProcess(SscsCaseData caseData,
                                            IdamTokens idamTokens, SscsCaseDetails caseDetails, EventType event,
                                            String userToken) {
        if (null != caseDetails) {
            sscsPdfService.generateAndSendPdf(caseData, caseDetails.getId(), idamTokens, "sscs1");
            if (event.equals(SYA_APPEAL_CREATED) || event.equals(VALID_APPEAL_CREATED)) {
                log.info("About to update case with sendToDwp event for id {}", caseDetails.getId());
                caseData.setDateSentToDwp(LocalDate.now().toString());
                ccdService.updateCase(caseData, caseDetails.getId(), SEND_TO_DWP.getCcdType(), "Send to DWP", "Send to DWP event has been triggered from Tribunals service", idamTokens);
                log.info("Case updated with sendToDwp event for id {}", caseDetails.getId());
            }
            if (StringUtils.isNotEmpty(userToken)) {
                citizenCcdService.draftArchived(caseData, getUserTokens(userToken), idamTokens);
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
            boolean moveToNoneCompliant = mrnDate.plusMonths(13L).isBefore(LocalDate.now());

            if (moveToNoneCompliant) {
                log.info("Moving case for NINO {} to non-compliant as MRN Date is older than 13 months", caseData.getAppeal().getAppellant().getIdentity().getNino());
                return NON_COMPLIANT;
            } else {
                log.info("Valid appeal to be created for case with NINO {}", caseData.getAppeal().getAppellant().getIdentity().getNino());
                return VALID_APPEAL_CREATED;
            }
        } else {
            log.info("Moving case for NINO {} to incomplete due to MRN Details {} present and MRN Date {} present",
                caseData.getAppeal().getAppellant().getIdentity().getNino(),
                (caseData.getAppeal().getMrnDetails() != null ? "" : "not"),
                (caseData.getAppeal().getMrnDetails().getMrnDate() != null ? "" : "not"));
            return INCOMPLETE_APPLICATION_RECEIVED;
        }
    }
}
