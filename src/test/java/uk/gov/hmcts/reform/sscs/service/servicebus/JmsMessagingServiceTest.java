package uk.gov.hmcts.reform.sscs.service.servicebus;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jms.core.JmsTemplate;
import uk.gov.hmcts.reform.sscs.model.servicebus.SessionAwareRequest;

@RunWith(MockitoJUnitRunner.class)
public class JmsMessagingServiceTest {

    @Mock
    private JmsTemplate jmsTemplate;

    @Mock
    private SessionAwareRequest message;

    private JmsMessagingService jmsMessagingService;

    private final String queueName = "queue";

    @Before
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