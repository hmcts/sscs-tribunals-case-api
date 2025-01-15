package uk.gov.hmcts.reform.sscs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.UNPROCESSABLE_ENTITY,
        reason = "One of the given document type is not accepted by Document Store")
public class UnsupportedDocumentTypeException extends RuntimeException {

    public UnsupportedDocumentTypeException(Throwable cause) {
        super(cause);
    }

}