package uk.gov.hmcts.reform.sscs.exception;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class DuplicateCaseException extends RuntimeException {
    public DuplicateCaseException(String message) {
        super(message);
    }

    public DuplicateCaseException(String message, Throwable ex) {
        super(message, ex);
    }
}
