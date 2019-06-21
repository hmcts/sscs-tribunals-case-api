package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseData;

public interface PreSubmitCallbackHandler<T extends CaseData> {

    boolean canHandle(Callback<T> callback);

    PreSubmitCallbackResponse<T> handle(Callback<T> callback);
}
