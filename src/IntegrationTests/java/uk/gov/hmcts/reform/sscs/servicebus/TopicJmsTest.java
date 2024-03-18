package uk.gov.hmcts.reform.sscs.servicebus;

import static org.junit.Assert.assertEquals;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicReference;
import javax.jms.*;
import org.apache.qpid.jms.JmsTopic;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.jms.core.JmsTemplate;
import uk.gov.hmcts.reform.sscs.service.servicebus.TopicPublisher;
import uk.gov.hmcts.reform.sscs.service.servicebus.messaging.MessagingConfig;


@Ignore("Need to create an exchange before you use it.")
public class TopicJmsTest {
    @ClassRule
    public static final EmbeddedInMemoryQpidBrokerRule QPID_BROKER_RULE = new EmbeddedInMemoryQpidBrokerRule();
    private static final String DESTINATION = "amq.topic";
    private static final String MESSAGE = "a message";

    private final MessagingConfig config = new MessagingConfig();
    private final ConnectionFactory connectionFactory = config.jmsConnectionFactory(
        "clientId", "guest", "guest",
        "amqp://localhost:8899?amqp.idleTimeout=120000"
            + "&amqp.saslMechanisms=PLAIN&transport.trustAll=true"
            + "&transport.verifyHost=false", null
    );
    private final JmsTemplate jmsTemplate = config.jmsTemplate(connectionFactory);


    public TopicJmsTest() throws KeyManagementException, NoSuchAlgorithmException {
        System.setProperty("qpid.tests.mms.messagestore.persistence", "true");
    }

    @Test
    public void testMessageIsSent() throws JMSException {

        Connection connection = connectionFactory.createConnection();
        connection.start();
        jmsTemplate.setExplicitQosEnabled(true);
        jmsTemplate.setTimeToLive(10000L);
        final TopicPublisher publisher = new TopicPublisher(jmsTemplate, DESTINATION, connectionFactory);
        publisher.sendMessage(MESSAGE, "1", new AtomicReference<>());

        Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
        MessageConsumer subscriber = session.createDurableSubscriber(new JmsTopic(DESTINATION), "sub1");
        Message message = subscriber.receive(1000);

        session.commit();
        assertEquals("strings should be equal", MESSAGE, message.getBody(String.class));
        connection.stop();
        connection.close();

    }

}
