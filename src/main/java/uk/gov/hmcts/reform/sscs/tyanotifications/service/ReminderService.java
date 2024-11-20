package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.reminder.ReminderHandler;

@Service
public class ReminderService {

    private List<ReminderHandler> reminderHandlers;

    ReminderService(List<ReminderHandler> reminderHandlers) {
        this.reminderHandlers = reminderHandlers;
    }

    void createReminders(NotificationWrapper wrapper) {
        for (ReminderHandler reminderHandler : reminderHandlers) {
            if (reminderHandler.canHandle(wrapper) && reminderHandler.canSchedule(wrapper)) {
                reminderHandler.handle(wrapper);
            }
        }
    }

}
