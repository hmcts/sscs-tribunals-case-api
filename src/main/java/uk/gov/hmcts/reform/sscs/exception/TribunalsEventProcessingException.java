package uk.gov.hmcts.reform.sscs.exception;

import java.io.Serial;

public class TribunalsEventProcessingException extends Exception {

    @Serial
    private static final long serialVersionUID = -2199850500845066670L;

    public TribunalsEventProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public TribunalsEventProcessingException(String message) {
        super(message);
    }

}
