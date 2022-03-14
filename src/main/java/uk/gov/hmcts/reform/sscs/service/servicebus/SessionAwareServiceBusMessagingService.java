package uk.gov.hmcts.reform.sscs.service.servicebus;

import com.azure.spring.integration.core.AzureHeaders;
import com.azure.spring.integration.servicebus.converter.ServiceBusMessageHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Sinks;
import uk.gov.hmcts.reform.sscs.model.hearings.SessionAwareRequest;

@Slf4j
public class SessionAwareServiceBusMessagingService implements SessionAwareMessagingService {

    private final Sinks.Many<Message<SessionAwareRequest>> eventSink;

    public SessionAwareServiceBusMessagingService(
        Sinks.Many<Message<SessionAwareRequest>> eventSink) {
        this.eventSink = eventSink;
    }

    public boolean sendMessage(SessionAwareRequest message) {

        log.info("About to emit request: {}", message);

        try {
            eventSink.emitNext(MessageBuilder.withPayload(message)
                    .setHeader(ServiceBusMessageHeaders.SESSION_ID, message.getSessionId())
                    .setHeader(AzureHeaders.PARTITION_KEY, message.getSessionId())
                    .build(),
                Sinks.EmitFailureHandler.FAIL_FAST);
        } catch (Exception ex) {
            log.error("Unable to send message {}. Cause: {}", message, ex);

            return false;
        }

        return true;
    }
}
