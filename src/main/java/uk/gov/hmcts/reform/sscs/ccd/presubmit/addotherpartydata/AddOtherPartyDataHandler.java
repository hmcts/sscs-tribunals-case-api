package uk.gov.hmcts.reform.sscs.ccd.presubmit.addotherpartydata;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class AddOtherPartyDataHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callback != null && CallbackType.ABOUT_TO_SUBMIT.equals(callbackType)
            && EventType.ADD_OTHER_PARTY_DATA.equals(callback.getEvent());
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        var preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);

        if (caseData.getOtherParties().size() > 1) {
            preSubmitCallbackResponse.addError("Only one other party data can be added using this event!");
        }

        return preSubmitCallbackResponse;
    }
}
