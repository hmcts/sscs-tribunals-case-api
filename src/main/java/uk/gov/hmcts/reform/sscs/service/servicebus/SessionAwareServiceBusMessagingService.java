package uk.gov.hmcts.reform.sscs.service.servicebus;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.sscs.model.servicebus.SessionAwareMessagingService;
import uk.gov.hmcts.reform.sscs.model.servicebus.SessionAwareRequest;

@Slf4j
@RequiredArgsConstructor
public class SessionAwareServiceBusMessagingService implements SessionAwareMessagingService {

    private final ServiceBusSenderClient senderClient;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean sendMessage(SessionAwareRequest message) {
        try {
            ServiceBusMessage serviceBusMessage = new ServiceBusMessage(objectMapper.writeValueAsString(message));
            serviceBusMessage.setContentType(MediaType.APPLICATION_JSON_VALUE);
            serviceBusMessage.getApplicationProperties().put("_type", "uk.gov.hmcts.reform.sscs.model.hearings.HearingRequest");

            log.info("Azure : About to send request with body: {}", serviceBusMessage.getBody().toString());

            senderClient.sendMessage(serviceBusMessage);

        } catch (Exception ex) {
            log.error("Unable to send message {}. Cause: {}", message, ex);

            return false;
        }

        return true;
    }
}
