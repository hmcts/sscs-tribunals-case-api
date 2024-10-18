package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseData;

public interface PreSubmitCallbackHandler<T extends CaseData> {

    boolean canHandle(CallbackType callbackType, Callback<T> callback);

    PreSubmitCallbackResponse<T> handle(CallbackType callbackType, Callback<T> callback, String userAuthorisation);
}
