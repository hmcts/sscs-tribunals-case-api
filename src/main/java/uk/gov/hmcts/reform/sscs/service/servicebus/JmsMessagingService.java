package uk.gov.hmcts.reform.sscs.service.servicebus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.core.JmsTemplate;
import uk.gov.hmcts.reform.sscs.model.servicebus.SessionAwareMessagingService;
import uk.gov.hmcts.reform.sscs.model.servicebus.SessionAwareRequest;

@Slf4j
@RequiredArgsConstructor
public class JmsMessagingService implements SessionAwareMessagingService {

    private final JmsTemplate jmsTemplate;

    private final String tribunalsToHearingsQueue;

    public boolean sendMessage(SessionAwareRequest message) {

        try {
            log.info("JMS : About to send request with body: {}", message.toString());
            jmsTemplate.convertAndSend(tribunalsToHearingsQueue, message);
        } catch (Exception ex) {
            log.error("Unable to send message {}. Cause: {}", message, ex);
            return false;
        }
        return true;
    }
}
