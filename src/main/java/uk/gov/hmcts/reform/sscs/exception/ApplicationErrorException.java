package uk.gov.hmcts.reform.sscs.exception;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class ApplicationErrorException extends RuntimeException {
    public ApplicationErrorException(Throwable cause) {
        super(cause);
    }
}
