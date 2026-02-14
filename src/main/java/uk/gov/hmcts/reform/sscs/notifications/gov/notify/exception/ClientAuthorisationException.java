package uk.gov.hmcts.reform.sscs.notifications.gov.notify.exception;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class ClientAuthorisationException extends RuntimeException {

    public ClientAuthorisationException(Exception ex) {
        super(ex);
    }
}
