package uk.gov.hmcts.reform.sscs.service.servicebus;

import static org.mockito.Mockito.*;

import java.net.NoRouteToHostException;
import java.util.concurrent.atomic.AtomicReference;
import javax.jms.Message;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jms.IllegalStateException;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.connection.SingleConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessagePostProcessor;

@RunWith(MockitoJUnitRunner.class)
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

        Assert.assertEquals(messageId, msg.get().getJMSMessageID());
    }

    @Test(expected = NoRouteToHostException.class)
    public void recoverMessageThrowsThePassedException() throws Throwable {
        Exception exception = new NoRouteToHostException("");
        underTest.recoverMessage(exception);
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

    @Test(expected = Exception.class)
    public void sendMessageWhenOtherThrowException() {
        doThrow(Exception.class).when(jmsTemplate).convertAndSend(anyString(),any(), any());

        underTest.sendMessage("a message", "1", new AtomicReference<>());

        Assert.assertTrue(false);

    }
}
