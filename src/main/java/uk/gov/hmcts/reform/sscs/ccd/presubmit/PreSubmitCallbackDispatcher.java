package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.createcase.CreateCaseAboutToStartHandler.isCreateCaseStartCallback;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;

@Slf4j
@Component
public class PreSubmitCallbackDispatcher<T extends CaseData> {

    private final List<PreSubmitCallbackHandler<T>> callbackHandlers;

    public PreSubmitCallbackDispatcher(List<PreSubmitCallbackHandler<T>> callbackHandlers) {
        requireNonNull(callbackHandlers, "callbackHandlers must not be null");
        this.callbackHandlers = callbackHandlers;
    }

    public PreSubmitCallbackResponse<T> handle(CallbackType callbackType, Callback<T> callback, String userAuth) {
        requireNonNull(callback, "callback must not be null");

        List<PreSubmitCallbackHandler<T>> eligibleHandlers = new ArrayList<>();
        PreSubmitCallbackResponse<T> aggregateResponse =
                new PreSubmitCallbackResponse<>(callback.getCaseDetails().getCaseData());

        for (PreSubmitCallbackHandler<T> callbackHandler : callbackHandlers) {
            if (callbackHandler.canHandle(callbackType, callback)) {
                eligibleHandlers.add(callbackHandler);

                var updatedCallback = getUpdatedCallback(aggregateResponse.getData(), callback, callbackType);

                PreSubmitCallbackResponse<T> handlerResponse =
                        callbackHandler.handle(callbackType, updatedCallback, userAuth);

                aggregateResponse.setData(handlerResponse.getData());
                aggregateResponse.addErrors(handlerResponse.getErrors());
                aggregateResponse.addWarnings(handlerResponse.getWarnings());
            }
        }
        //TODO revert to this line (eligibleHandlers.size() > 1) {
        if (!eligibleHandlers.isEmpty()) {
            log.info("{} has more than one handler {}", callback.getEvent(), eligibleHandlers);
        }
        return aggregateResponse;
    }

    private Callback<T> getUpdatedCallback(T caseData, Callback<T> callback, CallbackType callbackType) {
        boolean isCreateCaseStartCallback = isCreateCaseStartCallback(callbackType, callback.getEvent());
        var updatedCaseDetails = new CaseDetails<>(
                callback.getCaseDetails().getId(),
                callback.getCaseDetails().getJurisdiction(),
                isCreateCaseStartCallback ? null : callback.getCaseDetails().getState(),
                caseData,
                isCreateCaseStartCallback ? null : callback.getCaseDetails().getCreatedDate(),
                callback.getCaseDetails().getCaseTypeId()
        );
        var updatedCallback = new Callback<>(
                updatedCaseDetails, callback.getCaseDetailsBefore(), callback.getEvent(), callback.isIgnoreWarnings()
        );
        updatedCallback.setPageId(callback.getPageId());
        return updatedCallback;
    }
}
