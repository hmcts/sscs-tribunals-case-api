package uk.gov.hmcts.reform.sscs.ccd.presubmit.amendhearingoutcome;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Slf4j
@Component
public class AmendHearingOutcomeMidEventHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType.equals(CallbackType.MID_EVENT)
                && callback.getEvent().equals(EventType.AMEND_HEARING_OUTCOME);

    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }
        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse = new PreSubmitCallbackResponse<>(sscsCaseData);
        boolean hasNewOutcomeBeenAdded = sscsCaseData.getHearingOutcomes().stream()
                .anyMatch(hearingOutcome -> hearingOutcome.getValue().getCompletedHearingId() == null);
        if (hasNewOutcomeBeenAdded) {
            preSubmitCallbackResponse.addError("You cannot create a new hearing outcome in Amend Hearing Outcome, select  ‘Add New hearing outcome’ event to add another outcome.");
        }

        return preSubmitCallbackResponse;
    }
}
