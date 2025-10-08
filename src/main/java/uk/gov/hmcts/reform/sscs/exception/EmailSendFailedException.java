package uk.gov.hmcts.reform.sscs.exception;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class EmailSendFailedException extends RuntimeException {

    public EmailSendFailedException(String message, Throwable cause) {
        super(cause);
    }
}
