package uk.gov.hmcts.reform.sscs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class FileToPdfConversionException extends RuntimeException {
    public FileToPdfConversionException(String message, Throwable e) {
        super(message, e);
    }
}
