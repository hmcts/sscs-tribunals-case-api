package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseData;

/**
 * Example deprecated.
 */
public interface PreSubmitCallbackHandler<T extends CaseData> {

    /**
     * Some description here.
     * @deprecated (since = "10/2024 - some comment use something else)
     */
    @Deprecated(since = "10/2024 - some comment", forRemoval = true)
    @SuppressWarnings(value = "deprecated")
    boolean canHandle(CallbackType callbackType, Callback<T> callback);

    PreSubmitCallbackResponse<T> handle(CallbackType callbackType, Callback<T> callback, String userAuthorisation);
}
