package uk.gov.hmcts.reform.sscs.service.servicebus;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.core.dependencies.google.gson.Gson;
import javax.jms.Destination;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.model.servicebus.SessionAwareMessagingService;
import uk.gov.hmcts.reform.sscs.model.servicebus.SessionAwareRequest;

@Slf4j
@RequiredArgsConstructor
public class SessionAwareServiceBusMessagingService implements SessionAwareMessagingService {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ServiceBusSenderClient senderClient;

    @Override
    public boolean sendMessage(SessionAwareRequest message) {

        try {
            ServiceBusMessage serviceBusMessage = new ServiceBusMessage(objectMapper.writeValueAsString(message));
            serviceBusMessage.setSessionId(message.getSessionId());
            serviceBusMessage.setPartitionKey(message.getSessionId());
            serviceBusMessage.setContentType(MediaType.APPLICATION_JSON_VALUE);

            log.info("Azure : About to send request with body: {}", serviceBusMessage.getBody().toString());

            senderClient.sendMessage(serviceBusMessage);

        } catch (Exception ex) {
            log.error("Unable to send message {}. Cause: {}", message, ex);
            return false;
        }
        return true;
    }
}
