package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.RetryContext;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.Correspondence;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.model.LetterType;
import uk.gov.hmcts.reform.sscs.service.CcdNotificationsPdfService;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.LetterAsyncConfigProperties;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

@Slf4j
@Component
public class SaveCorrespondenceAsyncService {
    private final CcdNotificationsPdfService ccdNotificationsPdfService;

    public SaveCorrespondenceAsyncService(CcdNotificationsPdfService ccdNotificationsPdfService) {
        this.ccdNotificationsPdfService = ccdNotificationsPdfService;
    }

    @Autowired
    private LetterAsyncConfigProperties letterAsyncConfigProperties;

    @Async
    @Retryable(maxAttemptsExpression = "#{@letterAsyncConfigProperties.maxAttempts}", backoff = @Backoff(delayExpression = "#{@letterAsyncConfigProperties.delay}", multiplierExpression = "#{@letterAsyncConfigProperties.multiplier}", random = true, maxDelayExpression = "#{@letterAsyncConfigProperties.maxDelay}"))
    public void saveLetter(NotificationClient client, String notificationId, Correspondence correspondence, String ccdCaseId) throws NotificationClientException {

        RetryContext context = RetrySynchronizationManager.getContext();
        if (context != null && context.getRetryCount() == 0) {
            log.debug("delaying by {} milliseconds before making first attempt to get letter pdf for case id : {}",letterAsyncConfigProperties.getInitialDelay(), ccdCaseId);
            try {
                // Using  Thread.sleep here as it's already running in async and not blocking end user requests. Using CompletableFuture is too complex for this.
                Thread.sleep(letterAsyncConfigProperties.getInitialDelay());
            } catch (InterruptedException e) {
                log.warn("Thread was interrupted while applying a sleep to get letter pdf for case id : {} ", ccdCaseId);
                Thread.currentThread().interrupt();
            }
        }
        try {
            final byte[] pdfForLetter = client.getPdfForLetter(notificationId);
            log.info("Using merge letter correspondence V2 to upload letter correspondence for {} ", ccdCaseId);
            ccdNotificationsPdfService.mergeLetterCorrespondenceIntoCcdV2(pdfForLetter, Long.valueOf(ccdCaseId), correspondence);
        } catch (NotificationClientException e) {
            if (e.getMessage().contains("PDFNotReadyError")) {
                log.info("Got a PDFNotReadyError back from gov.notify for case id: {}.", ccdCaseId);
            } else {
                log.warn("Got a strange error '{}' back from gov.notify for case id: {}.", e.getMessage(), ccdCaseId);
            }
            throw e;
        }
    }

    @Async
    @Retryable(maxAttemptsExpression = "#{@letterAsyncConfigProperties.maxAttempts}", backoff = @Backoff(delayExpression = "#{@letterAsyncConfigProperties.delay}", multiplierExpression = "#{@letterAsyncConfigProperties.multiplier}", random = true))
    public void saveLetter(final byte[] pdfForLetter, Correspondence correspondence, String ccdCaseId, SubscriptionType subscriptionType) {
        log.info("Using notification letter correspondence V2 to upload reasonable adjustments correspondence for {} ", ccdCaseId);
        ccdNotificationsPdfService.mergeReasonableAdjustmentsCorrespondenceIntoCcdV2(pdfForLetter, Long.valueOf(ccdCaseId), correspondence, LetterType.findLetterTypeFromSubscription(subscriptionType.name()));
    }

    @Retryable
    public void saveEmailOrSms(final Correspondence correspondence, final SscsCaseData sscsCaseData) {
        int retry = (RetrySynchronizationManager.getContext() != null) ? RetrySynchronizationManager.getContext().getRetryCount() + 1 : 1;
        log.info("Retry number {} : to upload correspondence for {}, case reference {}",
            retry, correspondence.getValue().getCorrespondenceType().name(), sscsCaseData.getCcdCaseId());

        ccdNotificationsPdfService.mergeCorrespondenceIntoCcdV2(Long.valueOf(sscsCaseData.getCcdCaseId()), correspondence);
    }

    @Recover
    @SuppressWarnings({"unused"})
    public void getBackendResponseFallback(Throwable e) {
        log.error("Failed saving correspondence.", e);
    }
}