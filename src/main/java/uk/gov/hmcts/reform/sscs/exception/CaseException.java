package uk.gov.hmcts.reform.sscs.exception;

import java.io.Serial;

public class CaseException extends Exception {
    @Serial
    private static final long serialVersionUID = -3977477574548786807L;

    public CaseException(String message) {
        super(message);
    }
}
