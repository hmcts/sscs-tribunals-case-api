package uk.gov.hmcts.reform.sscs.notifications.gov.notify.exception;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class MacException extends RuntimeException {

    public MacException(Exception ex) {
        super(ex);
    }
}
