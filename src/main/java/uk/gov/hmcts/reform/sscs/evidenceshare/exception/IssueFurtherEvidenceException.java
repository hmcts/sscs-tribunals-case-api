package uk.gov.hmcts.reform.sscs.evidenceshare.exception;

public class IssueFurtherEvidenceException extends RuntimeException {

    private static final long serialVersionUID = 5150112073795926371L;

    public IssueFurtherEvidenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
