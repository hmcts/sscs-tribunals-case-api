package uk.gov.hmcts.reform.sscs.hearings.exception;

public class InvalidHmcMessageException extends MessageProcessingException {
    private static final long serialVersionUID = 3892214875734169729L;

    public InvalidHmcMessageException(String message) {
        super(message);
    }
}
