package uk.gov.hmcts.reform.sscs.exception;

public class OrchestratorJsonException extends RuntimeException {
    private static final long serialVersionUID = 1641102126427991237L;

    public OrchestratorJsonException(Throwable throwable) {
        super(throwable);
    }
}
