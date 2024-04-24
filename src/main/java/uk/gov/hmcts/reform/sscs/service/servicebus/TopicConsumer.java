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

@Slf4j
@Component
@Lazy(false)
public class TopicConsumer {

    private final Integer maxRetryAttempts;
    private final CallbackDispatcher<SscsCaseData> dispatcher;
    private final SscsCaseCallbackDeserializer sscsDeserializer;

    public TopicConsumer(@Value("${send-letter.maxRetryAttempts}") Integer maxRetryAttempts,
                         CallbackDispatcher<SscsCaseData> dispatcher,
                         SscsCaseCallbackDeserializer sscsDeserializer) {
        this.maxRetryAttempts = maxRetryAttempts;
        //noinspection unchecked
        this.dispatcher = dispatcher;
        this.sscsDeserializer = sscsDeserializer;
    }


    @JmsListener(
        destination = "${amqp.topic}",
        containerFactory = "topicJmsListenerContainerFactory",
        subscription = "${amqp.subscription}"
    )
    public void onMessage(String message, @Header(JmsHeaders.MESSAGE_ID) String messageId) {
        processMessageWithRetry(message, 1, messageId);
    }

    private void processMessageWithRetry(String message, int retry, String messageId) {
        try {
            log.info("Message Id {} received from the service bus by evidence share service", messageId);
            processMessage(message, messageId);
        } catch (Exception e) {
            if (retry > maxRetryAttempts || isException(e)) {
                log.error(format("Caught unknown unrecoverable error %s for message id %s", e.getMessage(), messageId), e);
            } else {
                log.info("Statcktrace is {}", e.getStackTrace());
                log.info(String.format("Caught recoverable error %s, retrying %s out of %s for message id %s",
                    e.getMessage(), retry, maxRetryAttempts, messageId));

                processMessageWithRetry(message, retry + 1, messageId);
            }
        }
    }

    private boolean isException(Exception e) {
        return e instanceof IssueFurtherEvidenceException || e instanceof PostIssueFurtherEvidenceTasksException;
    }

    private void processMessage(String message, String messageId) {
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
