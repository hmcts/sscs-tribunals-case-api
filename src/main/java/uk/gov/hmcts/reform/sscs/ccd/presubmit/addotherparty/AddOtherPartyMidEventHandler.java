package uk.gov.hmcts.reform.sscs.ccd.presubmit.addotherparty;

import static java.util.Objects.nonNull;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class AddOtherPartyMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return nonNull(callback) && CallbackType.MID_EVENT.equals(callbackType)
            && EventType.ADD_OTHER_PARTY_DATA.equals(callback.getEvent())
            && nonNull(callback.getCaseDetails().getCaseData().getOtherParties());
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
