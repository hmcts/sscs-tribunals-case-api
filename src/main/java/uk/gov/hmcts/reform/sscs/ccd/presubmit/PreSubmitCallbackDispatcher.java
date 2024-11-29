package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.util.Objects.requireNonNull;

import java.util.List;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseData;

@Component
public class PreSubmitCallbackDispatcher<T extends CaseData> {

    private final List<PreSubmitCallbackHandler<T>> callbackHandlers;

    public PreSubmitCallbackDispatcher(List<PreSubmitCallbackHandler<T>> callbackHandlers) {
        requireNonNull(callbackHandlers, "callbackHandlers must not be null");
        this.callbackHandlers = callbackHandlers;
    }

    public PreSubmitCallbackResponse<T> handle(CallbackType callbackType, Callback<T> callback, String userAuthorisation) {

        requireNonNull(callback, "callback must not be null");
        T caseData = callback.getCaseDetails().getCaseData();

        PreSubmitCallbackResponse<T> callbackResponse = new PreSubmitCallbackResponse<>(caseData);

        dispatchToHandlers(callbackType, callback, callbackHandlers, callbackResponse, userAuthorisation);

        return callbackResponse;
    }

    private void dispatchToHandlers(CallbackType callbackType, Callback<T> callback,
                                    List<PreSubmitCallbackHandler<T>> callbackHandlers,
                                    PreSubmitCallbackResponse<T> callbackResponse,
                                    String userAuthorisation
                                    ) {

        for (PreSubmitCallbackHandler<T> callbackHandler : callbackHandlers) {
            if (callbackHandler.canHandle(callbackType, callback)) {

                PreSubmitCallbackResponse<T> callbackResponseFromHandler = callbackHandler.handle(callbackType, callback, userAuthorisation);

                callbackResponse.setData(callbackResponseFromHandler.getData());
                callbackResponse.addErrors(callbackResponseFromHandler.getErrors());
                callbackResponse.addWarnings(callbackResponseFromHandler.getWarnings());
            }
        }
    }
}
