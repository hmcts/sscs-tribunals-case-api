package uk.gov.hmcts.reform.sscs.ccd.presubmit.canceltranslations;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class CancelTranslationsAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private static Map<String, String> nextEventMap = new HashMap<>();

    static {
        nextEventMap.put(DocumentType.SSCS1.getValue(), EventType.SEND_TO_DWP.getCcdType());
        nextEventMap.put(DocumentType.URGENT_HEARING_REQUEST.getValue(), EventType.UPDATE_CASE_ONLY.getCcdType());
        nextEventMap.put(DocumentType.DECISION_NOTICE.getValue(), EventType.DECISION_ISSUED_WELSH.getCcdType());
        nextEventMap.put(DocumentType.DIRECTION_NOTICE.getValue(), EventType.DIRECTION_ISSUED_WELSH.getCcdType());
        nextEventMap.put(DocumentType.REINSTATEMENT_REQUEST.getValue(), EventType.UPDATE_CASE_ONLY.getCcdType());
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbackType must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent().equals(EventType.CANCEL_TRANSLATIONS);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        log.info("Can handle for case id : {}", caseData.getCcdCaseId());
        if (!callback.getCaseDetails().getState().equals(State.INTERLOCUTORY_REVIEW_STATE)) {
            setWelshNextEvent(caseData);
            log.info("Set Welsh next event to : {} for case id: {}", caseData.getSscsWelshPreviewNextEvent(), caseData.getCcdCaseId());
        } else {
            InterlocReviewState interlocState = Arrays.stream(InterlocReviewState.values())
                .filter(x -> x.getCcdDefinition().equals(caseData.getWelshInterlocNextReviewState()))
                .findFirst()
                .orElse(null);
            caseData.setInterlocReviewState(interlocState);
            caseData.setWelshInterlocNextReviewState(null);
            log.info("Set InterlocReviewState : {} for case id: {}", caseData.getInterlocReviewState(), caseData.getCcdCaseId());
        }
        clearTranslationRequiredDocumentStatuses(caseData);
        caseData.updateTranslationWorkOutstandingFlag();
        log.info("Cleared tranlsation reqd statuses on docs and updated translation Ouststanding flag  for case id : {}", caseData.getCcdCaseId());
        return new PreSubmitCallbackResponse<>(caseData);
    }

    private void clearTranslationRequiredDocumentStatuses(SscsCaseData caseData) {
        caseData.getSscsDocument().stream().filter(sd -> SscsDocumentTranslationStatus.TRANSLATION_REQUIRED
                .equals(sd.getValue().getDocumentTranslationStatus()) || SscsDocumentTranslationStatus.TRANSLATION_REQUESTED
                .equals(sd.getValue().getDocumentTranslationStatus()))
                .forEach(td -> {
                    td.getValue().setDocumentTranslationStatus(null);
                });
    }

    private void setWelshNextEvent(SscsCaseData caseData) {
        caseData.getSscsDocument().stream().filter(sd ->
                (SscsDocumentTranslationStatus.TRANSLATION_REQUIRED.equals(sd.getValue().getDocumentTranslationStatus())
                        || SscsDocumentTranslationStatus.TRANSLATION_REQUESTED.equals(sd.getValue().getDocumentTranslationStatus()))
                        && nextEventMap.keySet().contains(sd.getValue().getDocumentType())).sorted().findFirst()
                .ifPresent(sscsDocument -> caseData
                        .setSscsWelshPreviewNextEvent(nextEventMap.get(sscsDocument.getValue().getDocumentType())));
    }
}
