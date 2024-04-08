package uk.gov.hmcts.reform.sscs.callback;

import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseData;

public interface CallbackHandler<T extends CaseData> {
    boolean canHandle(CallbackType callbackType, Callback<T> callback);

    void handle(CallbackType callbackType, Callback<T> callback);

    DispatchPriority getPriority();
}
