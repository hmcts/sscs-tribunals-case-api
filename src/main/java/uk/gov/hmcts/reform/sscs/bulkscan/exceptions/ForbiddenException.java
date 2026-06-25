package uk.gov.hmcts.reform.sscs.bulkscan.exceptions;

import java.io.Serial;

public class ForbiddenException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1641102126427991709L;

    public ForbiddenException(String message) {
        super(message);
    }
}
