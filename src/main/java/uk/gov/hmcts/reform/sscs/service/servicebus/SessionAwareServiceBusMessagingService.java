package uk.gov.hmcts.reform.sscs.service.servicebus;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.core.dependencies.google.gson.Gson;
import javax.jms.Destination;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import uk.gov.hmcts.reform.sscs.model.servicebus.SessionAwareMessagingService;
import uk.gov.hmcts.reform.sscs.model.servicebus.SessionAwareRequest;

@Slf4j
@RequiredArgsConstructor
public class SessionAwareServiceBusMessagingService implements SessionAwareMessagingService {

    private final ServiceBusSenderClient senderClient;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final JmsTemplate jmsTemplate;


    public boolean sendMessage(SessionAwareRequest message) {

        try {

            //ServiceBusMessage serviceBusMessage = new ServiceBusMessage(objectMapper.writeValueAsString(message));
            jmsTemplate.convertAndSend("tribunals-to-hearings-queue", message);


        } catch (Exception ex) {
            log.error("Unable to send message {}. Cause: {}", message, ex);

            return false;
        }

        return true;
    }
}
