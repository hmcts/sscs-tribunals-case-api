package uk.gov.hmcts.sscs.exception;

public class ApplicationErrorException extends RuntimeException {
    public ApplicationErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
