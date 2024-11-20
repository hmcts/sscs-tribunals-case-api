package uk.gov.hmcts.reform.sscs.exception;

import java.io.Serial;

public class OrchestratorJsonException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1641102126427991237L;

    public OrchestratorJsonException(Throwable throwable) {
        super(throwable);
    }
}
