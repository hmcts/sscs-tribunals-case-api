package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.fehandledoffline;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class FeHandledOfflineHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callback != null && EventType.FURTHER_EVIDENCE_HANDLED_OFFLINE == callback.getEvent()
            && CallbackType.ABOUT_TO_SUBMIT == callbackType;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        return null;
    }
}
