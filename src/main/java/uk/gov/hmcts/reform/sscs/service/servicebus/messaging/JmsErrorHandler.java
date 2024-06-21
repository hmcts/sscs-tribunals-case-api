package uk.gov.hmcts.reform.sscs.service.servicebus.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.util.ErrorHandler;

@Slf4j
public class JmsErrorHandler implements ErrorHandler {

    @Override
    public void handleError(@NonNull Throwable throwable) {
        log.warn("Spring JMS custom error handling triggered");

        // Log the full error message and stack trace
        log.error("Error occurred: {}", throwable.getMessage(), throwable);

        // Log the root cause if available
        Throwable rootCause = getRootCause(throwable);
        if (rootCause != null) {
            log.error("Root cause: {}", rootCause.getMessage(), rootCause);
        }
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause != cause.getCause()) {
            cause = cause.getCause();
        }
        return cause;
    }
}
