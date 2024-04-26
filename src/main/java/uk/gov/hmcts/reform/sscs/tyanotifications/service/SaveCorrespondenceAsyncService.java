package uk.gov.hmcts.reform.sscs.tyanotifications.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import uk.gov.hmcts.reform.sscs.tyanotifications.config.SubscriptionType;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;

@Slf4j
@Component
@SuppressWarnings("squid:S6857")
public class SaveCorrespondenceAsyncService {
    private final CcdNotificationsPdfService ccdNotificationsPdfService;

    @Autowired
    public SaveCorrespondenceAsyncService(CcdNotificationsPdfService ccdNotificationsPdfService) {
        this.ccdNotificationsPdfService = ccdNotificationsPdfService;
    }

    @Async
    @Retryable(maxAttemptsExpression = "#{${letterAsync.maxAttempts}}", backoff = @Backoff(delayExpression = "#{${letterAsync.delay}}", multiplierExpression = "#{${letterAsync.multiplier}}", random = true))
    public void saveLetter(NotificationClient client, String notificationId, Correspondence correspondence, String ccdCaseId) throws NotificationClientException {
        try {
            log.info("Getting PDF for letter correspondence for notification id {} ", notificationId);
            final byte[] pdfForLetter = client.getPdfForLetter(notificationId);
            log.info("Using merge letter correspondence V2 to upload letter correspondence for {} ", ccdCaseId);
            ccdNotificationsPdfService.mergeLetterCorrespondenceIntoCcdV2(pdfForLetter, Long.valueOf(ccdCaseId), correspondence);
        } catch (NotificationClientException e) {
            if (e.getMessage().contains("PDFNotReadyError")) {
                e.printStackTrace();
                log.info("Got a PDFNotReadyError back from gov.notify for case id: {}.", ccdCaseId);
            } else {
                log.warn("Got a strange error '{}' back from gov.notify for case id: {}.", e.getMessage(), ccdCaseId);
            }
            throw e;
        }
    }

    @Async
    @Retryable(maxAttemptsExpression = "#{${letterAsync.maxAttempts}}", backoff = @Backoff(delayExpression = "#{${letterAsync.delay}}", multiplierExpression = "#{${letterAsync.multiplier}}", random = true))
    public void saveLetter(Correspondence correspondence, final byte[] pdfForLetter, String ccdCaseId) {
        ccdNotificationsPdfService.mergeLetterCorrespondenceIntoCcd(pdfForLetter, Long.valueOf(ccdCaseId), correspondence);
    }

    @Async
    @Retryable(maxAttemptsExpression = "#{${letterAsync.maxAttempts}}", backoff = @Backoff(delayExpression = "#{${letterAsync.delay}}", multiplierExpression = "#{${letterAsync.multiplier}}", random = true))
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
