package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static java.util.Objects.nonNull;

import java.time.ZonedDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.TyanRetryConfig;
import uk.gov.hmcts.reform.sscs.tyanotifications.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;
import uk.gov.service.notify.NotificationClientException;

@Service
@Slf4j
public class RetryNotificationService {
    private NotificationHandler notificationHandler;
    private TyanRetryConfig tyanRetryConfig;

    RetryNotificationService(NotificationHandler notificationHandler, TyanRetryConfig tyanRetryConfig) {
        this.notificationHandler = notificationHandler;
        this.tyanRetryConfig = tyanRetryConfig;
    }

    public void rescheduleIfHandledGovNotifyErrorStatus(final int retry,
                                                        final NotificationWrapper notificationWrapper,
                                                        final NotificationServiceException e) {
        if (nonNull(e.getCause()) && e.getCause() instanceof NotificationClientException) {
            int httpResult = ((NotificationClientException) e.getCause()).getHttpResult();
            if (retry > 0 && retry <= tyanRetryConfig.getMax() && httpResult != 400 && httpResult != 403) {
                Integer delayInSeconds = tyanRetryConfig.getDelayInSeconds().get(retry);
                ZonedDateTime dateTime = ZonedDateTime.now().plusSeconds(delayInSeconds);
                log.info("Retry {} is rescheduling in {} seconds for case id {} and event id {}", retry, delayInSeconds, notificationWrapper.getCaseId(), notificationWrapper.getNotificationType().getId());
                notificationHandler.scheduleNotification(notificationWrapper, retry, dateTime);
            }
        }
    }
}
