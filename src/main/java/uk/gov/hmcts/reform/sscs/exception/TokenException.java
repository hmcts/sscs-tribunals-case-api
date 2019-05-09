package uk.gov.hmcts.reform.sscs.exception;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class TokenException extends RuntimeException {
    public TokenException(Throwable cause) {
        super(cause);
    }
}