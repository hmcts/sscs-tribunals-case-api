package uk.gov.hmcts.reform.sscs.ccd.presubmit.addotherparty;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.UC;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.AWAIT_OTHER_PARTY_DATA;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.WITH_DWP;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
class AddOtherPartyAboutToStartEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final boolean cmConfidentialityEnabled;

    public AddOtherPartyAboutToStartEventHandler(
        @Value("${feature.cm-other-party-confidentiality.enabled}") final boolean cmConfidentialityEnabled) {
        this.cmConfidentialityEnabled = cmConfidentialityEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callbackType, "callbackType must not be null");
        requireNonNull(callback, "callback must not be null");

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        return callbackType == CallbackType.ABOUT_TO_START
            && callback.getEvent() == EventType.ADD_OTHER_PARTY_DATA
            && cmConfidentialityEnabled
            && (caseData.isBenefitType(CHILD_SUPPORT) || caseData.isBenefitType(UC));
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
        String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        var preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(caseData);

        if (caseData.isBenefitType(CHILD_SUPPORT) && callback.getCaseDetails().getState() != AWAIT_OTHER_PARTY_DATA) {
            preSubmitCallbackResponse.addError(
                "The case must be at state \"Await Other Party Data\" in order to add another party");
        }

        if (caseData.isBenefitType(UC) && callback.getCaseDetails().getState() != WITH_DWP) {
            preSubmitCallbackResponse.addError("The case must be at state \"With FTA\" in order to add another party");
        }

        return preSubmitCallbackResponse;
    }
}
