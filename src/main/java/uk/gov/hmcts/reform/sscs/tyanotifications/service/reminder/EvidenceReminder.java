package uk.gov.hmcts.reform.sscs.tyanotifications.service.reminder;

import static org.slf4j.LoggerFactory.getLogger;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.EVIDENCE_REMINDER;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.DWP_RESPONSE_RECEIVED;
import static uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType.DWP_UPLOAD_RESPONSE;

import java.time.ZonedDateTime;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;
import uk.gov.hmcts.reform.sscs.tyanotifications.extractor.DwpResponseReceivedDateExtractor;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;

@Component
public class EvidenceReminder implements ReminderHandler {

    private static final org.slf4j.Logger LOG = getLogger(EvidenceReminder.class);

    private final DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor;
    private final JobGroupGenerator jobGroupGenerator;
    private final JobScheduler jobScheduler;
    private final long evidenceReminderDelay;

    public EvidenceReminder(
        DwpResponseReceivedDateExtractor dwpResponseReceivedDateExtractor,
        JobGroupGenerator jobGroupGenerator,
        JobScheduler jobScheduler,
        @Value("${reminder.evidenceReminder.delay.seconds}") long evidenceReminderDelay
    ) {
        this.dwpResponseReceivedDateExtractor = dwpResponseReceivedDateExtractor;
        this.jobGroupGenerator = jobGroupGenerator;
        this.jobScheduler = jobScheduler;
        this.evidenceReminderDelay = evidenceReminderDelay;
    }

    public boolean canHandle(NotificationWrapper wrapper) {
        return wrapper
            .getNotificationType()
            .equals(DWP_RESPONSE_RECEIVED)
            || wrapper.getNotificationType().equals(DWP_UPLOAD_RESPONSE);
    }

    public boolean canSchedule(NotificationWrapper wrapper) {
        boolean isReminderDatePresent = false;
        try {
            isReminderDatePresent = calculateReminderDate(wrapper.getNewSscsCaseData()) != null;
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

        SscsCaseData caseData = wrapper.getNewSscsCaseData();
        String caseId = caseData.getCcdCaseId();
        String eventId = EVIDENCE_REMINDER.getCcdType();
        String jobGroup = jobGroupGenerator.generate(caseId, eventId);
        ZonedDateTime reminderDate = calculateReminderDate(caseData);

        if (reminderDate != null) {
            jobScheduler.schedule(new Job<>(
                jobGroup,
                eventId,
                caseId,
                reminderDate
            ));

            LOG.info("Scheduled evidence reminder for case id: {} @ {}", caseId, reminderDate);
        } else {
            LOG.info("Could not find reminder date for case id {}", wrapper.getNewSscsCaseData().getCcdCaseId());
        }
    }

    private ZonedDateTime calculateReminderDate(SscsCaseData ccdResponse) {

        Optional<ZonedDateTime> dwpResponseReceivedDate = dwpResponseReceivedDateExtractor.extract(ccdResponse);

        if (dwpResponseReceivedDate.isPresent()) {
            return dwpResponseReceivedDate.get()
                .plusSeconds(evidenceReminderDelay);
        }

        return null;
    }

}
