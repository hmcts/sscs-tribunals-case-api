package uk.gov.hmcts.reform.sscs.notifications.gov.notify.exception;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class ReminderException extends RuntimeException {

    public ReminderException(Exception ex) {
        super(ex);
    }
}
