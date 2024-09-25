package uk.gov.hmcts.reform.sscs.tyanotifications.exception;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class AuthorisationException extends RuntimeException {

    public AuthorisationException(Exception ex) {
        super(ex);
    }
}
