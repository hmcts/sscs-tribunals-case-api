package uk.gov.hmcts.reform.sscs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@SuppressWarnings("squid:MaximumInheritanceDepth")
@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
public class CaseAccessException extends RuntimeException {
    public CaseAccessException(String message) {
        super(message);
    }
}
