package uk.gov.hmcts.sscs.exception;

public class EmailSendFailedException extends RuntimeException {

    public EmailSendFailedException(Throwable cause) {
        super(cause);
    }
}
