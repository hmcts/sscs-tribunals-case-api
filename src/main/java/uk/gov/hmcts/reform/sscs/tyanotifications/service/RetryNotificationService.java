package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static java.util.Objects.nonNull;

import java.time.ZonedDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.RetryConfig;
import uk.gov.hmcts.reform.sscs.tyanotifications.exception.NotificationServiceException;
import uk.gov.hmcts.reform.sscs.tyanotifications.factory.NotificationWrapper;
import uk.gov.service.notify.NotificationClientException;

@Service
@Slf4j
public class RetryNotificationService {
    private NotificationHandler notificationHandler;
    private RetryConfig retryConfig;

    @Autowired
    RetryNotificationService(NotificationHandler notificationHandler, RetryConfig retryConfig) {
        this.notificationHandler = notificationHandler;
        this.retryConfig = retryConfig;
    }

    public void rescheduleIfHandledGovNotifyErrorStatus(final int retry,
                                                        final NotificationWrapper notificationWrapper,
                                                        final NotificationServiceException e) {
        if (nonNull(e.getCause()) && e.getCause() instanceof NotificationClientException) {
            int httpResult = ((NotificationClientException) e.getCause()).getHttpResult();
            if (retry > 0 && retry <= retryConfig.getMax() && httpResult != 400 && httpResult != 403) {
                Integer delayInSeconds = retryConfig.getDelayInSeconds().get(retry);
                ZonedDateTime dateTime = ZonedDateTime.now().plusSeconds(delayInSeconds);
                log.info("Retry {} is rescheduling in {} seconds for case id {} and event id {}", retry, delayInSeconds, notificationWrapper.getCaseId(), notificationWrapper.getNotificationType().getId());
                notificationHandler.scheduleNotification(notificationWrapper, retry, dateTime);
            }
        }
    }
}
