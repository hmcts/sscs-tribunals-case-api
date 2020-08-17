package uk.gov.hmcts.reform.sscs.ccd.presubmit.canceltranslations;

import static java.util.Objects.requireNonNull;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
@Slf4j
public class CancelTranslationsAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

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
        for (SscsDocument sscsDocument : caseData.getSscsDocument()) {
            if (sscsDocument.getValue().getDocumentTranslationStatus() != null
                    && sscsDocument.getValue().getDocumentTranslationStatus().equals(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED)) {
                if (sscsDocument.getValue().getDocumentType() != null
                        && (sscsDocument.getValue().getDocumentType().equalsIgnoreCase("appellantEvidence")
                        || sscsDocument.getValue().getDocumentType().equalsIgnoreCase("Decision Notice")
                        || sscsDocument.getValue().getDocumentType().equalsIgnoreCase("Direction Notice")
                        || sscsDocument.getValue().getDocumentType().equalsIgnoreCase("sscs1"))) {
                    sscsDocument.getValue().setDocumentTranslationStatus(null);
                }
            }
        }
        caseData.setTranslationWorkOutstanding("No");
        return new PreSubmitCallbackResponse<>(caseData);
    }


}
