package uk.gov.hmcts.reform.sscs.callback;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseData;

@Component
@Slf4j
public class CallbackDispatcher<T extends CaseData> {

    private final List<CallbackHandler<T>> callbackHandlers;

    public CallbackDispatcher(List<CallbackHandler<T>> callbackHandlers) {
        requireNonNull(callbackHandlers, "callbackHandlers must not be null");
        this.callbackHandlers = callbackHandlers;
    }

    public void handle(CallbackType callbackType, Callback<T> callback) {
        requireNonNull(callback, "callback must not be null");
        Stream.of(DispatchPriority.values())
            .forEach(dispatchPriority ->
                dispatchToHandlers(callbackType, callback, getCallbackHandlersByPriority(dispatchPriority)));
    }

    private List<CallbackHandler<T>> getCallbackHandlersByPriority(DispatchPriority dispatchPriority) {
        return callbackHandlers.stream()
            .filter(handler -> handler.getPriority() == dispatchPriority)
            .collect(Collectors.toList());
    }

    private void dispatchToHandlers(CallbackType callbackType, Callback<T> callback,
                                    List<CallbackHandler<T>> callbackHandlers) {
        log.info("Dispatching callback of type {} to {} handlers", callbackType, callbackHandlers.size());
        callbackHandlers.stream()
            .filter(handler -> handler.canHandle(callbackType, callback))
            .forEach(handler -> handler.handle(callbackType, callback));
    }
}
