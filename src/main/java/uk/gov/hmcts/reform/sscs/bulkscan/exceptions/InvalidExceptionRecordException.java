package uk.gov.hmcts.reform.sscs.bulkscan.exceptions;

import java.util.List;

public class InvalidExceptionRecordException extends RuntimeException {

    private final List<String> errors;

    public InvalidExceptionRecordException(List<String> errors) {
        super("Validation errors: " + String.join(", ", errors));
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
