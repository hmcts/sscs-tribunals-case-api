package uk.gov.hmcts.reform.sscs.bulkscan.exceptions;

import java.io.Serial;

public class UnauthorizedException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -948721106877408028L;

    public UnauthorizedException(String message) {
        super(message);
    }
}
