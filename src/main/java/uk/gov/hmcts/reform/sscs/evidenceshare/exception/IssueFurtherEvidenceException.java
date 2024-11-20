package uk.gov.hmcts.reform.sscs.evidenceshare.exception;

import java.io.Serial;

public class IssueFurtherEvidenceException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 5150112073795926371L;

    public IssueFurtherEvidenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
