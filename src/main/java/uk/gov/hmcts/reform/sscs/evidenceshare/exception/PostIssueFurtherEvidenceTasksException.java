package uk.gov.hmcts.reform.sscs.evidenceshare.exception;

import java.io.Serial;

public class PostIssueFurtherEvidenceTasksException extends RuntimeException {


    @Serial
    private static final long serialVersionUID = -1131392893302438644L;

    public PostIssueFurtherEvidenceTasksException(String message, Throwable cause) {
        super(message, cause);
    }
}
