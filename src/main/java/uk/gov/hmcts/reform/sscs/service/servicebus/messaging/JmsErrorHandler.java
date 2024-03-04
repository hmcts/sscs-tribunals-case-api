package uk.gov.hmcts.reform.sscs.service.servicebus.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.util.ErrorHandler;

@Slf4j
public class JmsErrorHandler implements ErrorHandler {

    @Override
    public void handleError(@NonNull Throwable throwable) {
        log.warn("spring jms custom error handling example");
        log.error(throwable.getCause().getMessage());
    }
}
