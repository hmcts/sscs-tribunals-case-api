package uk.gov.hmcts.reform.sscs.tyanotifications.service.reminder;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.HEARING_BOOKED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.HEARING_REMINDER;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.AppConstants;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.AppealHearingType;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;

@Component
public class HearingReminder implements ReminderHandler {

    private static final org.slf4j.Logger LOG = getLogger(HearingReminder.class);

    private final JobGroupGenerator jobGroupGenerator;
    private JobScheduler jobScheduler;

    private long beforeFirstHearingReminder;
    private long beforeSecondHearingReminder;

    public HearingReminder(
        JobGroupGenerator jobGroupGenerator,
        JobScheduler jobScheduler,
        @Value("${reminder.hearingReminder.beforeFirst.seconds}") long beforeFirstHearingReminder,
        @Value("${reminder.hearingReminder.beforeSecond.seconds}") long beforeSecondHearingReminder
    ) {
        this.jobGroupGenerator = jobGroupGenerator;
        this.jobScheduler = jobScheduler;
        this.beforeFirstHearingReminder = beforeFirstHearingReminder;
        this.beforeSecondHearingReminder = beforeSecondHearingReminder;
    }

    public boolean canHandle(NotificationWrapper wrapper) {
        return wrapper
            .getNotificationType()
            .equals(HEARING_BOOKED) && isAllowedForHearingType(wrapper.getHearingType());
    }

    private boolean isAllowedForHearingType(AppealHearingType hearingType) {
        return ((AppealHearingType.PAPER.equals(hearingType) && HEARING_REMINDER.isSendForPaperCase())
            || (AppealHearingType.ORAL.equals(hearingType) && HEARING_REMINDER.isSendForOralCase()));
    }

    public boolean canSchedule(NotificationWrapper wrapper) {
        boolean isReminderDatePresent = false;
        try {
            isReminderDatePresent = calculateReminderDate(wrapper.getNewSscsCaseData(), beforeFirstHearingReminder) != null;
        } catch (Exception e) {
            LOG.error("Error while calculating reminder date for case id {} with exception {}",
                wrapper.getNewSscsCaseData().getCcdCaseId(), e);
        }
        if (!isReminderDatePresent) {
            LOG.info("Could not find reminder date for case id {}", wrapper.getNewSscsCaseData().getCcdCaseId());
        }
        return isReminderDatePresent;
    }

    public void handle(NotificationWrapper wrapper) {
        if (!canHandle(wrapper)) {
            throw new IllegalArgumentException("cannot handle ccdResponse");
        }

        scheduleReminder(wrapper.getNewSscsCaseData(), beforeFirstHearingReminder);
        scheduleReminder(wrapper.getNewSscsCaseData(), beforeSecondHearingReminder);
    }

    private void scheduleReminder(SscsCaseData ccdResponse, long secondsBeforeHearing) {

        String caseId = ccdResponse.getCcdCaseId();
        String eventId = HEARING_REMINDER.getId();
        String jobGroup = jobGroupGenerator.generate(caseId, eventId);
        ZonedDateTime reminderDate = calculateReminderDate(ccdResponse, secondsBeforeHearing);

        if (reminderDate != null) {
            jobScheduler.schedule(new Job<>(
                jobGroup,
                eventId,
                caseId,
                reminderDate
            ));


            LOG.info("Scheduled hearing reminder for case id: {} @ {}", caseId, reminderDate);
        } else {
            LOG.info("Could not find reminder date for case id {}", ccdResponse.getCcdCaseId());
        }
    }

    private ZonedDateTime calculateReminderDate(SscsCaseData ccdResponse, long secondsBeforeHearing) {

        if (!ccdResponse.getHearings().isEmpty()) {
            Hearing hearing = ccdResponse.getHearings().get(0);
            LocalDateTime dateBefore = hearing.getValue().getHearingDateTime().minusSeconds(secondsBeforeHearing);
            return ZonedDateTime.ofLocal(dateBefore, ZoneId.of(AppConstants.ZONE_ID), null);
        }

        return null;
    }

}
