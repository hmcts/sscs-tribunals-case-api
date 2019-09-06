package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class UploadDocumentsHandler implements PreSubmitCallbackHandler {
    @Override
    public boolean canHandle(CallbackType callbackType, Callback callback) {
        return callbackType != null && callback != null && callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent().equals(EventType.UPLOAD_DOCUMENT);
    }

    @Override
    public PreSubmitCallbackResponse handle(CallbackType callbackType, Callback callback) {
        return null;
    }
}
