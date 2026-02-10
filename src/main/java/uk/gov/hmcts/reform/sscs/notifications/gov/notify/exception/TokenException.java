package uk.gov.hmcts.reform.sscs.notifications.gov.notify.exception;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class TokenException extends RuntimeException {

    public TokenException(Exception ex) {
        super(ex);
    }
}
