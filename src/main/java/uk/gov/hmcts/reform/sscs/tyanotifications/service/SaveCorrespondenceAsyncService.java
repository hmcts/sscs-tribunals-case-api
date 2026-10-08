package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import static java.lang.Long.valueOf;
import static uk.gov.hmcts.reform.sscs.model.LetterType.findLetterTypeFromSubscription;

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
import uk.gov.hmcts.reform.sscs.service.CcdNotificationsPdfService;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.LetterAsyncConfigProperties;
import uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

@Slf4j
@Component
public class SaveCorrespondenceAsyncService {
    private final CcdNotificationsPdfService ccdNotificationsPdfService;

    @Autowired
    public SaveCorrespondenceAsyncService(CcdNotificationsPdfService ccdNotificationsPdfService) {
        this.ccdNotificationsPdfService = ccdNotificationsPdfService;
    }

    @Autowired
    private LetterAsyncConfigProperties letterAsyncConfigProperties;

    @Async
    @Retryable(recover = "recoverSaveLetter", maxAttemptsExpression = "#{@letterAsyncConfigProperties.maxAttempts}", backoff = @Backoff(delayExpression = "#{@letterAsyncConfigProperties.delay}", multiplierExpression = "#{@letterAsyncConfigProperties.multiplier}", random = true, maxDelayExpression = "#{@letterAsyncConfigProperties.maxDelay}"))
    public void saveLetter(NotificationClient client, String notificationId, Correspondence correspondence,
                           String ccdCaseId) throws NotificationClientException {

        RetryContext context = RetrySynchronizationManager.getContext();
        if (context != null && context.getRetryCount() == 0) {
            log.debug("delaying by {} milliseconds before making first attempt to get letter pdf for case id : {}",
                    letterAsyncConfigProperties.getInitialDelay(), ccdCaseId);
            try {
                // Using  Thread.sleep here as it's already running in async and not blocking end user requests. Using CompletableFuture is too complex for this.
                Thread.sleep(letterAsyncConfigProperties.getInitialDelay());
            } catch (InterruptedException e) {
                log.warn("Thread was interrupted while applying a sleep to get letter pdf for case ({}) ", ccdCaseId);
                Thread.currentThread().interrupt();
            }
        }
        try {
            final byte[] pdfForLetter = client.getPdfForLetter(notificationId);
            log.info("Using merge letter correspondence V2 to upload letter correspondence for {} ", ccdCaseId);
            ccdNotificationsPdfService
                    .mergeLetterCorrespondenceIntoCcdV2(pdfForLetter, valueOf(ccdCaseId), correspondence);
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
    @Retryable(recover = "recoverSaveBulkPrintLetter", maxAttemptsExpression = "#{@letterAsyncConfigProperties.maxAttempts}",
            backoff = @Backoff(delayExpression = "#{@letterAsyncConfigProperties.delay}", multiplierExpression = "#{@letterAsyncConfigProperties.multiplier}",
                    maxDelayExpression = "#{@letterAsyncConfigProperties.maxDelay}", random = true))
    public void saveLetter(byte[] pdfForLetter, Correspondence correspondence, String ccdCaseId) {
        log.info("Using mergeLetterCorrespondenceV2 to upload BulkPrint sent letter correspondence for {} ", ccdCaseId);
        ccdNotificationsPdfService
                .mergeLetterCorrespondenceIntoCcdV2(pdfForLetter, valueOf(ccdCaseId), correspondence, "Bulk Print");
    }

    @Recover
    @SuppressWarnings({"unused"})
    public void recoverSaveLetter(Throwable e, NotificationClient client, String notificationId,
                                  Correspondence correspondence, String ccdCaseId) {
        log.error("Failed to get letter pdf from gov.notify or save it into ccd for notification id {} and case id {} "
                        + "after retries exhausted, notification was sent but will not appear on the Notifications Sent tab.",
                notificationId, ccdCaseId, e);
        ccdNotificationsPdfService.notifyFailedToRetrieveCorrespondence(valueOf(ccdCaseId), notificationId, correspondence.getValue().getCorrespondenceType());
    }

    @Recover
    @SuppressWarnings({"unused"})
    public void recoverSaveBulkPrintLetter(Throwable e, byte[] pdfForLetter, Correspondence correspondence, String ccdCaseId) {
        log.error("Failed saving {} correspondence into ccd for case id {} after retries exhausted, "
                        + "notification was sent but will not appear on the Notifications Sent tab.",
                correspondence.getValue().getCorrespondenceType(), ccdCaseId, e);
        ccdNotificationsPdfService.notifyFailedToRetrieveCorrespondence(valueOf(ccdCaseId), null, correspondence.getValue().getCorrespondenceType());
    }

    @Async
    @Retryable(recover = "recoverSaveLettersToReasonableAdjustment", maxAttemptsExpression = "#{@letterAsyncConfigProperties.maxAttempts}",
            backoff = @Backoff(delayExpression = "#{@letterAsyncConfigProperties.delay}", multiplierExpression = "#{@letterAsyncConfigProperties.multiplier}",
                    maxDelayExpression = "#{@letterAsyncConfigProperties.maxDelay}", random = true))
    public void saveLettersToReasonableAdjustment(final byte[] pdfForLetter, Correspondence correspondence, String ccdCaseId,
                                                  SubscriptionType subscriptionType) {
        log.info("Using notification letter correspondence V2 to upload reasonable adjustments correspondence for {} ",
                ccdCaseId);
        ccdNotificationsPdfService.mergeReasonableAdjustmentsCorrespondenceIntoCcdV2(pdfForLetter,
                valueOf(ccdCaseId), correspondence, findLetterTypeFromSubscription(subscriptionType.name()));
    }

    @Recover
    @SuppressWarnings({"unused"})
    public void recoverSaveLettersToReasonableAdjustment(Throwable e, byte[] pdfForLetter, Correspondence correspondence,
                                            String ccdCaseId, SubscriptionType subscriptionType) {
        log.error("Failed saving {} correspondence into ccd for case id {} after retries exhausted, "
                        + "notification was sent but will not appear on the Notifications Sent tab.",
                correspondence.getValue().getCorrespondenceType(), ccdCaseId, e);
        ccdNotificationsPdfService.notifyFailedToRetrieveCorrespondence(valueOf(ccdCaseId), null, correspondence.getValue().getCorrespondenceType());
    }

    @Async
    @Retryable(recover = "recoverSaveEmailOrSms", maxAttemptsExpression = "#{@letterAsyncConfigProperties.emailMaxAttempts}",
            backoff = @Backoff(delayExpression = "#{@letterAsyncConfigProperties.emailDelay}", multiplierExpression = "#{@letterAsyncConfigProperties.emailMultiplier}",
                    maxDelayExpression = "#{@letterAsyncConfigProperties.emailMaxDelay}", random = true))
    public void saveEmailOrSms(final String notificationId, final Correspondence correspondence, final SscsCaseData sscsCaseData) {
        int retry = (RetrySynchronizationManager.getContext() != null) ? RetrySynchronizationManager.getContext().getRetryCount() + 1 : 1;
        log.info("Retry number {} : to upload {} correspondence for notification id {}, case reference {}",
            retry, correspondence.getValue().getCorrespondenceType().name(), notificationId, sscsCaseData.getCcdCaseId());

        ccdNotificationsPdfService.mergeCorrespondenceIntoCcdV2(valueOf(sscsCaseData.getCcdCaseId()), correspondence);
    }

    @Recover
    @SuppressWarnings({"unused"})
    public void recoverSaveEmailOrSms(Throwable e, String notificationId, Correspondence correspondence, SscsCaseData sscsCaseData) {
        log.error("Failed saving {} correspondence into ccd for case id {} after retries exhausted, notification id {}, "
                        + "notification was sent but will not appear on the Notifications Sent tab.",
                correspondence.getValue().getCorrespondenceType(), sscsCaseData.getCcdCaseId(), notificationId, e);
        ccdNotificationsPdfService.notifyFailedToRetrieveCorrespondence(valueOf(sscsCaseData.getCcdCaseId()), notificationId, correspondence.getValue().getCorrespondenceType());
    }
}
