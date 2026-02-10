package uk.gov.hmcts.reform.sscs.notifications.gov.notify.exception;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class AuthorisationException extends RuntimeException {

    public AuthorisationException(Exception ex) {
        super(ex);
    }
}
