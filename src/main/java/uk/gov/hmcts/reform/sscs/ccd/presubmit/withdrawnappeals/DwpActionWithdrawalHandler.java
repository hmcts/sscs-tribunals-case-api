package uk.gov.hmcts.reform.sscs.ccd.presubmit.withdrawnappeals;

import static uk.gov.hmcts.reform.sscs.ccd.presubmit.withdrawnappeals.DwpState.WITHDRAWAL_RECEIVED;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class DwpActionWithdrawalHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType != null && callback != null && callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent().equals(EventType.DWP_ACTION_WITHDRAWAL)
            && WITHDRAWAL_RECEIVED.getValue().equals(callback.getCaseDetails().getCaseData().getDwpState());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setDwpState(DwpState.WITHDRAWN.getValue());
        return new PreSubmitCallbackResponse<>(caseData);
    }
}
