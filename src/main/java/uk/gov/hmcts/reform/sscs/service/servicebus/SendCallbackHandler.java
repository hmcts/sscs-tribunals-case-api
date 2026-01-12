package uk.gov.hmcts.reform.sscs.service.servicebus;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.callback.CallbackDispatcher;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.BulkPrintException;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.IssueFurtherEvidenceException;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.NonPdfBulkPrintException;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.PdfStoreException;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.PostIssueFurtherEvidenceTasksException;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.UnableToContactThirdPartyException;
import uk.gov.hmcts.reform.sscs.exception.DwpAddressLookupException;
import uk.gov.hmcts.reform.sscs.exception.NoMrnDetailsException;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.servicebus.NotificationsMessageProcessor;

@Slf4j
@Component
public class SendCallbackHandler {

    private final Integer maxRetryAttempts;
    private final CallbackDispatcher<SscsCaseData> dispatcher;
    private final NotificationsMessageProcessor notificationsMessageProcessor;

    public SendCallbackHandler(@Value("${callback.maxRetryAttempts}") Integer maxRetryAttempts,
                               CallbackDispatcher<SscsCaseData> dispatcher,
                               NotificationsMessageProcessor notificationsMessageProcessor) {
        this.maxRetryAttempts = maxRetryAttempts;
        this.dispatcher = dispatcher;
        this.notificationsMessageProcessor = notificationsMessageProcessor;
    }

    @Async("applicationTaskExecutor")
    public void handle(Callback<SscsCaseData> callback) {
        log.info("Received message for case ID: {}, event: {}", callback.getCaseDetails().getId(), callback.getEvent());
        processEvidenceShareMessageWithRetry(callback, 1);
        notificationsMessageProcessor.processMessage(callback);
    }

    private void processEvidenceShareMessageWithRetry(Callback<SscsCaseData> callback, int retry) {
        try {
            processMessageForEvidenceShare(callback);
        } catch (Exception e) {
            if (retry > maxRetryAttempts || isException(e)) {
                log.error("Caught unknown unrecoverable error %s for callback {}, case ID: {}", callback.getEvent(), callback.getCaseDetails().getId(), e);
            } else {
                log.error("Caught recoverable error while retrying {} out of {} for callback {}, case ID: {}",
                    retry, maxRetryAttempts, callback.getEvent(), callback.getCaseDetails().getId(), e);
                processEvidenceShareMessageWithRetry(callback, retry + 1);
            }
        }
    }

    private boolean isException(Exception e) {
        return e instanceof IssueFurtherEvidenceException || e instanceof PostIssueFurtherEvidenceTasksException;
    }

    private void processMessageForEvidenceShare(Callback<SscsCaseData> callback) {
        try {
            dispatcher.handle(SUBMITTED, callback);
            log.info("Sscs Case CCD callback `{}` handled for Case ID `{}`", callback.getEvent(), callback.getCaseDetails().getId());
        } catch (NonPdfBulkPrintException
                 | UnableToContactThirdPartyException
                 | PdfStoreException
                 | BulkPrintException
                 | DwpAddressLookupException
                 | NoMrnDetailsException exception) {
            // unrecoverable. Catch to remove it from the queue.
            log.error(format("Caught unrecoverable error: %s for event %s on case: %s", exception.getMessage(), callback.getEvent(), callback.getCaseDetails().getId()), exception);
        }
    }
}


