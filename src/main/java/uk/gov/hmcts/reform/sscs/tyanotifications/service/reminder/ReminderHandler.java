package uk.gov.hmcts.reform.sscs.tyanotifications.service.reminder;

import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;

public interface ReminderHandler {

    boolean canHandle(NotificationWrapper wrapper);

    void handle(NotificationWrapper wrapper);

    boolean canSchedule(NotificationWrapper wrapper);
}
