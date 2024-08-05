package uk.gov.hmcts.reform.sscs.tyanotifications.exception;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class TokenException extends RuntimeException {

    public TokenException(Exception ex) {
        super(ex);
    }
}
