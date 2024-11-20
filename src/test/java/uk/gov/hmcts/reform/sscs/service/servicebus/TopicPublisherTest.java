package uk.gov.hmcts.reform.sscs.service.servicebus;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import jakarta.jms.Message;
import java.net.NoRouteToHostException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jms.IllegalStateException;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

@ExtendWith(MockitoExtension.class)
public class TopicPublisherTest {

    private static final String DESTINATION = "Bermuda";
    private final JmsTemplate jmsTemplate = mock(JmsTemplate.class);
    private final CachingConnectionFactory connectionFactory = mock(CachingConnectionFactory.class);
    private TopicPublisher underTest = new TopicPublisher(jmsTemplate, DESTINATION, connectionFactory);
    private Message message = mock(Message.class);

    @Captor
    private ArgumentCaptor<MessagePostProcessor> lambdaCaptor;

    @Test
    public void sendMessageCallsTheJmsTemplateAndSetsMessageId() throws Exception {

        final AtomicReference<Message> msg = new AtomicReference<>();

        String messageId = "id:123";

        when(message.getJMSMessageID()).thenReturn(messageId);

        doAnswer((i) -> {

            msg.set(message);
            return null;
        }).when(jmsTemplate).convertAndSend(anyString(), anyString(), any());

        underTest.sendMessage("a message", "1", msg);

        verify(jmsTemplate).convertAndSend(eq(DESTINATION), any(), lambdaCaptor.capture());
        MessagePostProcessor lambda = lambdaCaptor.getValue();
        lambda.postProcessMessage(message);

        Assertions.assertEquals(messageId, msg.get().getJMSMessageID());
    }

    @Test
    public void recoverMessageThrowsThePassedException() {
        assertThrows(NoRouteToHostException.class, () -> {
            Exception exception = new NoRouteToHostException("");
            underTest.recoverMessage(exception);
        });
    }

    @Test
    public void sendMessageWhenThrowException() {
        doThrow(IllegalStateException.class).when(jmsTemplate).convertAndSend(anyString(),any(), any());

        try {
            underTest.sendMessage("a message", "1", new AtomicReference<>());
        } catch (Exception e) {
            verify(connectionFactory).resetConnection();
            verify(jmsTemplate,times(2)).convertAndSend(eq(DESTINATION), any(), any());
        }

    }

    @Test
    public void sendMessageWhenThrowExceptionWhenConnectionFactoryInstanceDifferent() {
        SingleConnectionFactory connectionFactory = mock(SingleConnectionFactory.class);

        doThrow(IllegalStateException.class).when(jmsTemplate).convertAndSend(anyString(),any(), any());

        underTest = new TopicPublisher(jmsTemplate, DESTINATION, connectionFactory);

        try {
            underTest.sendMessage("a message", "1", new AtomicReference<>());
        } catch (Exception e) {
            verify(connectionFactory,never()).resetConnection();
            verify(jmsTemplate,times(1)).convertAndSend(eq(DESTINATION), any(), any());
        }

    }

    @Test
    public void sendMessageWhenOtherThrowException() {
        assertThrows(Exception.class, () -> {
            doThrow(Exception.class).when(jmsTemplate).convertAndSend(anyString(), any(), any());

            underTest.sendMessage("a message", "1", new AtomicReference<>());

            Assertions.assertTrue(false);

        });

    }
}
