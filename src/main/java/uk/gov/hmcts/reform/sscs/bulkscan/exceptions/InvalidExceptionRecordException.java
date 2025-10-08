package uk.gov.hmcts.reform.sscs.bulkscan.exceptions;

import java.util.List;
import lombok.Getter;

@Getter
public class InvalidExceptionRecordException extends RuntimeException {

    private final List<String> errors;

    public InvalidExceptionRecordException(List<String> errors) {
        super("Validation errors: " + String.join(", ", errors));
        this.errors = errors;
    }

}
