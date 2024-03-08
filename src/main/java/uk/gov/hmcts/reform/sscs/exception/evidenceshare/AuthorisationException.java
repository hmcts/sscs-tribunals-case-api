package uk.gov.hmcts.reform.sscs.exception.evidenceshare;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class AuthorisationException extends RuntimeException {

    public AuthorisationException(Exception ex) {
        super(ex);
    }
}
