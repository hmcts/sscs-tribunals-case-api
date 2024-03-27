package uk.gov.hmcts.reform.sscs.tyanotifications.callback;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.DispatchPriority;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.SscsCaseDataWrapper;

@Component
public class CallbackDispatcher {

    private final List<CallbackHandler> callbackHandlers;

    @Autowired
    public CallbackDispatcher(List<CallbackHandler> callbackHandlers) {
        requireNonNull(callbackHandlers, "callbackHandlers must not be null");
        this.callbackHandlers = callbackHandlers;
    }

    public void handle(SscsCaseDataWrapper sscsCaseDataWrapper) {
        Stream.of(DispatchPriority.values())
            .forEach(dispatchPriority ->
                dispatchToHandlers(sscsCaseDataWrapper, getCallbackHandlersByPriority(dispatchPriority)));
    }

    private List<CallbackHandler> getCallbackHandlersByPriority(DispatchPriority dispatchPriority) {
        return callbackHandlers.stream()
            .filter(handler -> handler.getPriority() == dispatchPriority)
            .collect(Collectors.toList());
    }

    private void dispatchToHandlers(SscsCaseDataWrapper sscsCaseDataWrapper,
                                    List<CallbackHandler> callbackHandlers) {
        callbackHandlers.stream()
            .filter(handler -> handler.canHandle(sscsCaseDataWrapper))
            .forEach(handler -> handler.handle(sscsCaseDataWrapper));
    }
}
