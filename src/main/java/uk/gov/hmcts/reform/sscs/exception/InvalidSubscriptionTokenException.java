package uk.gov.hmcts.reform.sscs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@SuppressWarnings("squid:MaximumInheritanceDepth")
@ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Given token is invalid")
public class InvalidSubscriptionTokenException extends RuntimeException {

    public InvalidSubscriptionTokenException(Throwable cause) {
        super(cause);
    }
}
