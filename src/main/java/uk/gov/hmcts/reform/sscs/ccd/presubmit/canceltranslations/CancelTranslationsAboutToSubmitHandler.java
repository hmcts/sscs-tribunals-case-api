package uk.gov.hmcts.reform.sscs.ccd.presubmit.canceltranslations;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class CancelTranslationsAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private static Map<String, String> nextEventMap = new HashMap<>();

    static {
        nextEventMap.put(DocumentType.SSCS1.getValue(), EventType.SEND_TO_DWP.getCcdType());
        nextEventMap.put(DocumentType.DECISION_NOTICE.getValue(), EventType.DECISION_ISSUED_WELSH.getCcdType());
        nextEventMap.put(DocumentType.DIRECTION_NOTICE.getValue(), EventType.DIRECTION_ISSUED_WELSH.getCcdType());
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

        setWelshNextEvent(caseData);

        clearTranslationRequiredDocumentStatuses(caseData);

        caseData.updateTranslationWorkOutstandingFlag();
        return new PreSubmitCallbackResponse<>(caseData);
    }

    private void clearTranslationRequiredDocumentStatuses(SscsCaseData caseData) {
        caseData.getSscsDocument().stream().filter(sd -> SscsDocumentTranslationStatus.TRANSLATION_REQUIRED
            .equals(sd.getValue().getDocumentTranslationStatus())).forEach(td -> {
            td.getValue().setDocumentTranslationStatus(null);
        });
    }

    private void setWelshNextEvent(SscsCaseData caseData) {
        caseData.getSscsDocument().stream().filter(sd ->
            SscsDocumentTranslationStatus.TRANSLATION_REQUIRED.equals(sd.getValue().getDocumentTranslationStatus()) &&
                nextEventMap.keySet().contains(sd.getValue().getDocumentType())).sorted().findFirst()
            .ifPresent(sscsDocument -> caseData
                .setSscsWelshPreviewNextEvent(nextEventMap.get(sscsDocument.getValue().getDocumentType())));
    }
}
