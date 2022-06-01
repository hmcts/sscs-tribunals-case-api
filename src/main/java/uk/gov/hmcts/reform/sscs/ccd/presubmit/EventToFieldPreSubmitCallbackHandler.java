package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.util.Map;
import java.util.Optional;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DecisionType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public abstract class EventToFieldPreSubmitCallbackHandler implements PreSubmitCallbackHandler<SscsCaseData> {
    private final Map<EventType, String> eventFieldMappings;

    EventToFieldPreSubmitCallbackHandler(Map<EventType, String> eventFieldMappings) {
        this.eventFieldMappings = eventFieldMappings;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        return eventFieldMappings.containsKey(callback.getEvent()) && callbackType.equals(ABOUT_TO_SUBMIT);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        Optional<PreSubmitCallbackResponse<SscsCaseData>> response = responseWithErrorCheck(callback);
        if (response.isPresent()) {
            return response.get();
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        return new PreSubmitCallbackResponse<>(setField(sscsCaseData, eventFieldMappings.get(callback.getEvent()),
            callback.getEvent()));
    }

    private Optional<PreSubmitCallbackResponse<SscsCaseData>> responseWithErrorCheck(Callback<SscsCaseData> callback) {
        if (EventType.ACTION_STRIKE_OUT.equals(callback.getEvent())) {
            String decisionType = callback.getCaseDetails().getCaseData().getDecisionType();
            if (!DecisionType.STRIKE_OUT.getValue().equals(decisionType)) {
                PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(
                    callback.getCaseDetails().getCaseData());
                response.addError("The decision type is not \"strike out\". We cannot proceed.");
                return Optional.of(response);
            }
        }
        return Optional.empty();
    }

    protected abstract SscsCaseData setField(SscsCaseData sscsCaseData, String newValue, EventType eventType);
}
