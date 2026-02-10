package uk.gov.hmcts.reform.sscs.notifications.gov.notify.exception;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class CcdUtilException extends RuntimeException {

    public CcdUtilException(Exception ex) {
        super(ex);
    }
}
