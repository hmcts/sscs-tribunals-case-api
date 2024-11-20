package uk.gov.hmcts.reform.sscs.exception;

import java.io.Serial;

public class HearingUpdateException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 8467659945506711935L;

    public HearingUpdateException(String message, Throwable cause) {
        super(message, cause);
    }
}
