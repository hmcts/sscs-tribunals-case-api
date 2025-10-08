package uk.gov.hmcts.reform.sscs.exception;

public class HearingUpdateException extends RuntimeException {

    private static final long serialVersionUID = 8467659945506711935L;

    public HearingUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
