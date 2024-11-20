package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import java.net.UnknownHostException;
import java.time.ZonedDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;
import uk.gov.hmcts.reform.sscs.tyanotifications.domain.notify.NotificationEventType;
import uk.gov.hmcts.reform.sscs.tyanotifications.exception.NotificationClientRuntimeException;
import uk.gov.hmcts.reform.sscs.tyanotifications.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.reminder.JobGroupGenerator;
import uk.gov.service.notify.NotificationClientException;

@Service
@Slf4j
public class NotificationHandler {

    private final OutOfHoursCalculator outOfHoursCalculator;
    private final JobScheduler jobScheduler;
    private final JobGroupGenerator jobGroupGenerator;

    public NotificationHandler(OutOfHoursCalculator outOfHoursCalculator, JobScheduler jobScheduler, JobGroupGenerator jobGroupGenerator) {
        this.outOfHoursCalculator = outOfHoursCalculator;
        this.jobScheduler = jobScheduler;
        this.jobGroupGenerator = jobGroupGenerator;
    }

    public boolean sendNotification(NotificationWrapper wrapper, String notificationTemplate, final String notificationType, SendNotification sendNotification) {
        final String caseId = wrapper.getCaseId();
        try {
            log.info("Sending {} template {} for case id: {}", notificationType, notificationTemplate, caseId);
            sendNotification.send();
            log.info("{} template {} sent for case id: {}", notificationType, notificationTemplate, caseId);
            return true;
        } catch (Exception ex) {
            log.error("Could not send {} notification for case id: {}", notificationType, wrapper.getCaseId());
            wrapAndThrowNotificationExceptionIfRequired(wrapper, notificationTemplate, ex);
        }

        return false;
    }

    public void scheduleNotification(NotificationWrapper wrapper) {
        scheduleNotification(wrapper, outOfHoursCalculator.getStartOfNextInHoursPeriod());
    }

    public void scheduleNotification(NotificationWrapper wrapper, int retry, ZonedDateTime dateTime) {
        final String caseId = wrapper.getCaseId();
        String eventId = wrapper.getNotificationType().getId();
        String jobGroup = jobGroupGenerator.generate(caseId, eventId);
        log.info("Scheduled retry {} - {} for case id: {} @ {}", retry, eventId, caseId, dateTime);

        jobScheduler.schedule(new Job<>(
            jobGroup,
            eventId,
            wrapper.getSchedulerPayload() + "," + retry,
            dateTime
        ));
    }

    public void scheduleNotification(NotificationWrapper wrapper, ZonedDateTime dateTime) {
        final String caseId = wrapper.getCaseId();
        String eventId = wrapper.getNotificationType().getId();
        String jobGroup = jobGroupGenerator.generate(caseId, eventId);
        log.info("Scheduled {} for case id: {} @ {}", eventId, caseId, dateTime);

        jobScheduler.schedule(new Job<>(
            jobGroup,
            eventId,
            wrapper.getSchedulerPayload(),
            dateTime
        ));
    }

    private void wrapAndThrowNotificationExceptionIfRequired(NotificationWrapper wrapper, String templateId, Exception ex) {
        String caseId = wrapper.getCaseId();
        NotificationEventType notificationType = wrapper.getNotificationType();
        if (ex.getCause() instanceof UnknownHostException) {
            NotificationClientRuntimeException exception = new NotificationClientRuntimeException(caseId, ex);
            log.error("Runtime error on GovUKNotify for case id: {}, template: {}, notification type: {}", caseId, templateId, notificationType);
            log.error("Exception for case id {}", caseId, exception);
            throw exception;
        } else {
            NotificationServiceException exception = new NotificationServiceException(caseId, ex);
            log.error("Error code {} on GovUKNotify for case id: {}, template: {}, notification type: {}", exception.getGovNotifyErrorCode(), caseId, templateId, notificationType);
            log.error("Exception for case id {}", caseId, exception);
            if (ex instanceof NotificationClientException) {
                throw exception;
            }
        }
    }

    @FunctionalInterface
    public interface SendNotification {
        void send() throws NotificationClientException;
    }
}
