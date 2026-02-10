package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseData;

public interface EvidenceCallbackHandler<T extends CaseData> {
    boolean canHandle(CallbackType callbackType, Callback<T> callback);

    void handle(CallbackType callbackType, Callback<T> callback);

    DispatchPriority getPriority();
}
