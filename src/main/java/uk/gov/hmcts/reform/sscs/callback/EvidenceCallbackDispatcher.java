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
import uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers.EvidenceCallbackHandler;

@Component
@Slf4j
public class EvidenceCallbackDispatcher<T extends CaseData> {

    private final List<EvidenceCallbackHandler<T>> callbackHandlers;

    public EvidenceCallbackDispatcher(List<EvidenceCallbackHandler<T>> callbackHandlers) {
        requireNonNull(callbackHandlers, "callbackHandlers must not be null");
        this.callbackHandlers = callbackHandlers;
    }

    public void handle(CallbackType callbackType, Callback<T> callback) {
        requireNonNull(callback, "callback must not be null");
        Stream.of(DispatchPriority.values())
            .forEach(dispatchPriority ->
                dispatchToHandlers(callbackType, callback, getCallbackHandlersByPriority(dispatchPriority)));
    }

    private List<EvidenceCallbackHandler<T>> getCallbackHandlersByPriority(DispatchPriority dispatchPriority) {
        return callbackHandlers.stream()
            .filter(handler -> handler.getPriority() == dispatchPriority)
            .collect(Collectors.toList());
    }

    private void dispatchToHandlers(CallbackType callbackType, Callback<T> callback,
                                    List<EvidenceCallbackHandler<T>> callbackHandlers) {
        log.info("Dispatching callback of type {} to {} handlers for case id {}",
                callbackType,
                callbackHandlers.size(),
                callback.getCaseDetails().getId());
        callbackHandlers.stream()
            .filter(handler -> handler.canHandle(callbackType, callback))
            .forEach(handler -> handler.handle(callbackType, callback));
    }
}
