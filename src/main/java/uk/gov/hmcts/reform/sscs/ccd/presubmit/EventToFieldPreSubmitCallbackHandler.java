package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.util.Map;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
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
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        final CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        final SscsCaseData sscsCaseData = caseDetails.getCaseData();

        final SscsCaseData updatedInterlocCaseData = setField(sscsCaseData, eventFieldMappings.get(callback.getEvent()),
                callback.getEvent());

        return new PreSubmitCallbackResponse<>(updatedInterlocCaseData);
    }

    protected abstract SscsCaseData setField(SscsCaseData sscsCaseData, String newValue, EventType eventType);
}
