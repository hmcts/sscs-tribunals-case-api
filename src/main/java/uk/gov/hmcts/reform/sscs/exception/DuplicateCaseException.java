package uk.gov.hmcts.reform.sscs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@SuppressWarnings("squid:MaximumInheritanceDepth")
@ResponseStatus(value = HttpStatus.CONFLICT)
public class DuplicateCaseException extends RuntimeException {
    public DuplicateCaseException(String message) {
        super(message);
    }
}
