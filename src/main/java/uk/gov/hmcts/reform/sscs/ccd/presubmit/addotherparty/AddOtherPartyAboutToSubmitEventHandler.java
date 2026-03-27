package uk.gov.hmcts.reform.sscs.ccd.presubmit.addotherparty;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.UC;
import static uk.gov.hmcts.reform.sscs.util.DateTimeUtils.generateDwpResponseDueDate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
class AddOtherPartyAboutToSubmitEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private final boolean cmConfidentialityEnabled;

    public AddOtherPartyAboutToSubmitEventHandler(
        @Value("${feature.cm-other-party-confidentiality.enabled}") final boolean cmConfidentialityEnabled) {
        this.cmConfidentialityEnabled = cmConfidentialityEnabled;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callbackType, "callbackType must not be null");
        requireNonNull(callback, "callback must not be null");

        return cmConfidentialityEnabled
            && callbackType == CallbackType.ABOUT_TO_SUBMIT
            && callback.getEvent() == EventType.ADD_OTHER_PARTY_DATA
            && callback.getCaseDetails() != null
            && callback.getCaseDetails().getCaseData() != null
            && callback.getCaseDetails().getCaseData().isBenefitType(UC);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
        String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        final SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setInterlocReviewState(InterlocReviewState.HEF_ISSUED);
        caseData.setDirectionDueDate(generateDwpResponseDueDate(21));
        return new PreSubmitCallbackResponse<>(caseData);
    }
}
