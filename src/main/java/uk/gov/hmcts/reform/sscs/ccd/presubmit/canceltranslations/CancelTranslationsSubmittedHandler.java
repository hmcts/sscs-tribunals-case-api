package uk.gov.hmcts.reform.sscs.ccd.presubmit.canceltranslations;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.REINSTATEMENT_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.URGENT_HEARING_REQUEST;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL;

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@Service
@Slf4j
public class CancelTranslationsSubmittedHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final CcdService ccdService;
    private final IdamService idamService;
    private final UpdateCcdCaseService updateCcdCaseService;

    public CancelTranslationsSubmittedHandler(CcdService ccdService,
                                              IdamService idamService,
                                              UpdateCcdCaseService updateCcdCaseService) {
        this.ccdService = ccdService;
        this.idamService = idamService;
        this.updateCcdCaseService = updateCcdCaseService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.SUBMITTED)
            && callback.getEvent().equals(EventType.CANCEL_TRANSLATIONS)
            && !callback.getCaseDetails().getState().equals(State.INTERLOCUTORY_REVIEW_STATE)
            && StringUtils.isNotEmpty(callback.getCaseDetails().getCaseData().getSscsWelshPreviewNextEvent());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        String sscsWelshPreviewNextEvent = caseData.getSscsWelshPreviewNextEvent();
        log.info("sscsWelshPreviewNextEvent is {}  for case id : {}", sscsWelshPreviewNextEvent, caseData.getCcdCaseId());
        caseData.setSscsWelshPreviewNextEvent(null);

        if (isValidUrgentDocument(callback.getCaseDetails().getCaseData())) {
            setMakeCaseUrgentTriggerEvent(callback.getCaseDetails().getId());
        } else if (isValidResinstatementRequestDocument(callback.getCaseDetails().getCaseData())) {

            updateForReinstatementRequestEvent(caseData, callback.getCaseDetails().getId(), sscsWelshPreviewNextEvent);

        } else {
            ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                            sscsWelshPreviewNextEvent, "Cancel welsh translations", "Cancel welsh translations",
                            idamService.getIdamTokens());
        }

        return new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());
    }

    private boolean isValidUrgentDocument(SscsCaseData caseData) {
        return (!"Yes".equalsIgnoreCase(caseData.getUrgentCase())
                && (StringUtils.isEmpty(caseData.getTranslationWorkOutstanding()) || "No".equalsIgnoreCase(caseData.getTranslationWorkOutstanding()))
                && (!CollectionUtils.isEmpty(caseData.getSscsDocument()) && caseData.getSscsDocument().stream().anyMatch(d -> URGENT_HEARING_REQUEST.getValue().equals(d.getValue().getDocumentType()))));
    }

    private boolean isValidResinstatementRequestDocument(SscsCaseData caseData) {
        return (StringUtils.isEmpty(caseData.getTranslationWorkOutstanding()) || "No".equalsIgnoreCase(caseData.getTranslationWorkOutstanding()))
                && caseData.getReinstatementOutcome() == null
                && (!CollectionUtils.isEmpty(caseData.getSscsDocument()) && caseData.getSscsDocument().stream().anyMatch(d -> REINSTATEMENT_REQUEST.getValue().equals(d.getValue().getDocumentType())));
    }

    private SscsCaseDetails setMakeCaseUrgentTriggerEvent(Long caseId) {
        log.info("Triggering makeCaseUrgent event using updateCaseV2 for {}", caseId);
        return updateCcdCaseService.updateCaseV2(
                caseId,
                EventType.MAKE_CASE_URGENT.getCcdType(),
                "Send a case to urgent hearing",
                OTHER_DOCUMENT_MANUAL.getLabel(),
                idamService.getIdamTokens(),
                sscsCaseData -> { }
        );
    }

    private SscsCaseDetails updateForReinstatementRequestEvent(
            SscsCaseData caseData, Long caseId,
            String sscsWelshPreviewNextEvent) {

        caseData.setReinstatementOutcome(RequestOutcome.IN_PROGRESS);
        caseData.setReinstatementRegistered(LocalDate.now());
        caseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE);

        return  ccdService.updateCase(caseData, caseId, sscsWelshPreviewNextEvent, "Set Reinstatement Request", "Set Reinstatement Request",
                idamService.getIdamTokens());
    }
}
