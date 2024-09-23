package uk.gov.hmcts.reform.sscs.service.servicebus;

import static java.lang.String.format;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.support.JmsHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.callback.CallbackDispatcher;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.deserialisation.SscsCaseCallbackDeserializer;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.*;
import uk.gov.hmcts.reform.sscs.exception.DwpAddressLookupException;
import uk.gov.hmcts.reform.sscs.exception.NoMrnDetailsException;
import uk.gov.hmcts.reform.sscs.tyanotifications.service.servicebus.NotificationsMessageProcessor;

@Slf4j
@Component
@Lazy(false)
public class TopicConsumer {

    private final Integer maxRetryAttempts;
    private final CallbackDispatcher<SscsCaseData> dispatcher;
    private final SscsCaseCallbackDeserializer sscsDeserializer;
    private final NotificationsMessageProcessor notificationsMessageProcessor;
    private final boolean isNotificationServiceBypassed;

    public TopicConsumer(@Value("${callback.maxRetryAttempts}") Integer maxRetryAttempts,
                         CallbackDispatcher<SscsCaseData> dispatcher,
                         SscsCaseCallbackDeserializer sscsDeserializer, NotificationsMessageProcessor notificationsMessageProcessor,
                         @Value("${feature.bypass-notifications-service.enabled:false}") boolean isNotificationServiceBypassed) {
        this.maxRetryAttempts = maxRetryAttempts;
        //noinspection unchecked
        this.dispatcher = dispatcher;
        this.sscsDeserializer = sscsDeserializer;
        this.notificationsMessageProcessor = notificationsMessageProcessor;
        this.isNotificationServiceBypassed = isNotificationServiceBypassed;
    }


    @JmsListener(
        destination = "${amqp.topic}",
        containerFactory = "topicJmsListenerContainerFactory",
        subscription = "${amqp.subscription}"
    )

    public void onMessage(String message, @Header(JmsHeaders.MESSAGE_ID) String messageId) {
        log.info("Message Id {} received from the service bus", messageId);
        processEvidenceShareMessageWithRetry(message, 1, messageId);

        log.info("Determining if notification service should be bypassed {} ", isNotificationServiceBypassed ? "Yes" : "No");
        if (isNotificationServiceBypassed) {
            log.info("Bypassing notification service for message id {}", messageId);
            notificationsMessageProcessor.processMessage(message, messageId);
        }
    }

    private void processEvidenceShareMessageWithRetry(String message, int retry, String messageId) {
        try {
            log.info("Message Id {} received from the service bus by evidence share service, attempt {}", messageId, retry);
            processMessageForEvidenceShare(message, messageId);
        } catch (Exception e) {
            if (retry > maxRetryAttempts || isException(e)) {
                log.error("Caught unknown unrecoverable error %s for message id {}", messageId, e);
            } else {
                log.error("Caught recoverable error while retrying {} out of {} for message id {}",
                    retry, maxRetryAttempts, messageId, e);
                processEvidenceShareMessageWithRetry(message, retry + 1, messageId);
            }
        }
    }

    private boolean isException(Exception e) {
        return e instanceof IssueFurtherEvidenceException || e instanceof PostIssueFurtherEvidenceTasksException;
    }

    private void processMessageForEvidenceShare(String message, String messageId) {
        try {
            Callback<SscsCaseData> callback = sscsDeserializer.deserialize(message);
            dispatcher.handle(SUBMITTED, callback);
            log.info("Sscs Case CCD callback `{}` handled for Case ID `{}` for message id {}", callback.getEvent(), callback.getCaseDetails().getId(), messageId);
        } catch (NonPdfBulkPrintException
                 | UnableToContactThirdPartyException
                 | PdfStoreException
                 | BulkPrintException
                 | DwpAddressLookupException
                 | NoMrnDetailsException exception) {
            // unrecoverable. Catch to remove it from the queue.
            log.error(format("Caught unrecoverable error: %s for message id %s", exception.getMessage(), messageId), exception);
        }
    }
}


