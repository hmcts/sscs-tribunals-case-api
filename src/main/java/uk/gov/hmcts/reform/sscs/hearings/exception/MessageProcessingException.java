package uk.gov.hmcts.reform.sscs.hearings.exception;

public class MessageProcessingException extends Exception {
    private static final long serialVersionUID = -1216639664216596788L;

    public MessageProcessingException(String message) {
        super(message);
    }
}
