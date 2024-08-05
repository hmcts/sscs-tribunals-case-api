package uk.gov.hmcts.reform.sscs.tyanotifications.service.reminder;

import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.*;

import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobNotFoundException;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobRemover;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;

@Component
@Slf4j
public class AllReminderRemover implements ReminderHandler {

    private static final List<NotificationEventType> NOTIFICATION_EVENT_TYPES =
        Arrays.asList(APPEAL_LAPSED, HMCTS_APPEAL_LAPSED, DWP_APPEAL_LAPSED, APPEAL_WITHDRAWN,
            ADMIN_APPEAL_WITHDRAWN, APPEAL_DORMANT, DECISION_ISSUED, ISSUE_FINAL_DECISION);

    private static final List<NotificationEventType> REMINDERS_TO_REMOVE =
        Arrays.asList(HEARING_REMINDER, EVIDENCE_RECEIVED);
    private final JobGroupGenerator jobGroupGenerator;
    private final JobRemover jobRemover;

    @Autowired
    public AllReminderRemover(
        JobGroupGenerator jobGroupGenerator,
        JobRemover jobRemover
    ) {
        this.jobGroupGenerator = jobGroupGenerator;
        this.jobRemover = jobRemover;
    }

    public boolean canHandle(NotificationWrapper wrapper) {
        return NOTIFICATION_EVENT_TYPES.contains(wrapper.getNotificationType());
    }

    public boolean canSchedule(NotificationWrapper wrapper) {
        return true;
    }

    public void handle(NotificationWrapper wrapper) {
        if (!canHandle(wrapper)) {
            throw new IllegalArgumentException("cannot handle ccdResponse");
        }

        String caseId = wrapper.getCaseId();
        for (NotificationEventType eventType : REMINDERS_TO_REMOVE) {
            removeReminder(caseId, eventType);
        }
    }

    private void removeReminder(String caseId, NotificationEventType eventType) {
        String jobGroup = jobGroupGenerator.generate(caseId, eventType.getId());
        try {
            jobRemover.removeGroup(jobGroup);
            log.info("Removed {} from case id: {}", eventType, caseId);

        } catch (JobNotFoundException ignore) {
            log.debug("{} for case id: {} could not be found", eventType, caseId);
        }
    }

}
