package uk.gov.hmcts.reform.sscs.service.servicebus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.core.JmsTemplate;
import uk.gov.hmcts.reform.sscs.model.servicebus.SessionAwareRequest;

@ExtendWith(MockitoExtension.class)
public class JmsMessagingServiceTest {

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private SessionAwareRequest message;

    private JmsMessagingService jmsMessagingService;

    private final String queueName = "queue";

    @BeforeEach
    public void setUp() {
        jmsMessagingService = new JmsMessagingService(jmsTemplate, queueName);
    }

    @Test
    public void sendMessage_success() {
        assertTrue(jmsMessagingService.sendMessage(message));
        verify(jmsTemplate, times(1)).convertAndSend(queueName, message);
    }

    @Test
    public void sendMessage_exception() {
        doThrow(new RuntimeException()).when(jmsTemplate).convertAndSend(queueName, message);
        assertFalse(jmsMessagingService.sendMessage(message));
    }
}