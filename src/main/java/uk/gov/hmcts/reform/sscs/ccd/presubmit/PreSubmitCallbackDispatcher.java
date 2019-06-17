package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseData;

@Component
public class PreSubmitCallbackDispatcher<T extends CaseData> {

    private final List<PreSubmitCallbackHandler<T>> callbackHandlers;

    public PreSubmitCallbackDispatcher(
        List<PreSubmitCallbackHandler<T>> callbackHandlers
    ) {
        requireNonNull(callbackHandlers, "callbackHandlers must not be null");
        this.callbackHandlers = callbackHandlers;
    }

    public PreSubmitCallbackResponse<T> handle(Callback<T> callback) {
        requireNonNull(callback, "callback must not be null");

        T caseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<T> callbackResponse =
                new PreSubmitCallbackResponse<>(caseData);

        dispatchToHandlers(callback, callbackHandlers, callbackResponse);

        return callbackResponse;
    }

    private void dispatchToHandlers(
        Callback<T> callback,
        List<PreSubmitCallbackHandler<T>> callbackHandlers,
        PreSubmitCallbackResponse<T> callbackResponse
    ) {

        for (PreSubmitCallbackHandler<T> callbackHandler : callbackHandlers) {

            if (callbackHandler.canHandle(callback)) {

                PreSubmitCallbackResponse<T> callbackResponseFromHandler = callbackHandler.handle(callback);

                callbackResponse.setData(callbackResponseFromHandler.getData());

                if (!callbackResponseFromHandler.getErrors().isEmpty()) {
                    callbackResponse.addErrors(callbackResponseFromHandler.getErrors());
                }
            }
        }
    }
}
