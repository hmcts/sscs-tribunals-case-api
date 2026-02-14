package uk.gov.hmcts.reform.sscs.notifications.gov.notify.service.reminder;

import uk.gov.hmcts.reform.sscs.notifications.gov.notify.factory.NotificationWrapper;

public interface ReminderHandler {

    boolean canHandle(NotificationWrapper wrapper);

    void handle(NotificationWrapper wrapper);

    boolean canSchedule(NotificationWrapper wrapper);
}
