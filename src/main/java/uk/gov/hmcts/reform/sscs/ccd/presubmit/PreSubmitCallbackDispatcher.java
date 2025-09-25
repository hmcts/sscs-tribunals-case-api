package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;

@Slf4j
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
        List<PreSubmitCallbackHandler<T>> eligibleHandlers = new ArrayList<>();
        State callbackState = null;
        try {
            callbackState = callback.getCaseDetails().getState();
        } catch (Exception e) {
            log.info("============================================================");
            log.info("State is null for event={}, callbackType={}", callback.getEvent(), callbackType);
            log.info("============================================================");
        }

        for (PreSubmitCallbackHandler<T> callbackHandler : callbackHandlers) {
            if (callbackHandler.canHandle(callbackType, callback)) {

                PreSubmitCallbackResponse<T> callbackResponseFromHandler = callbackHandler.handle(callbackType, callback, userAuthorisation);

                callbackResponse.setData(callbackResponseFromHandler.getData());
                callbackResponse.addErrors(callbackResponseFromHandler.getErrors());
                callbackResponse.addWarnings(callbackResponseFromHandler.getWarnings());
                eligibleHandlers.add(callbackHandler);
            }
        }
        if (eligibleHandlers.size() > 1) {
            log.info("{} has more than one handler {}", callback.getEvent(), eligibleHandlers);
        }
    }
}
