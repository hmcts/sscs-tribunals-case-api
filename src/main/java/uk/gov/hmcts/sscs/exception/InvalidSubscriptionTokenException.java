package uk.gov.hmcts.sscs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.hmcts.reform.logging.exception.AlertLevel;
import uk.gov.hmcts.reform.logging.exception.UnknownErrorCodeException;

@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Given token is invalid")
public class InvalidSubscriptionTokenException extends UnknownErrorCodeException {

    public InvalidSubscriptionTokenException(Throwable cause) {
        super(AlertLevel.P3, cause);
    }
}
