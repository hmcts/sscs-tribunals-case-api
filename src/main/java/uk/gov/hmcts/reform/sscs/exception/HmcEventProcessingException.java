package uk.gov.hmcts.reform.sscs.exception;

public class HmcEventProcessingException extends Exception {

    private static final long serialVersionUID = -7206765397565397123L;

    public HmcEventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
