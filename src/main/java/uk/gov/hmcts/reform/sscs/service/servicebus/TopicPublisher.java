package uk.gov.hmcts.reform.sscs.service.servicebus;

import java.util.concurrent.atomic.AtomicReference;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jms.IllegalStateException;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TopicPublisher {

    private final JmsTemplate jmsTemplate;

    private final String destination;

    private final ConnectionFactory connectionFactory;

    @Autowired
    public TopicPublisher(JmsTemplate jmsTemplate,
                          @Value("${amqp.topic}") final String destination,
                          ConnectionFactory connectionFactory) {
        this.jmsTemplate = jmsTemplate;
        this.destination = destination;
        this.connectionFactory = connectionFactory;
    }

    @Retryable(
        maxAttempts = 5,
        backoff = @Backoff(delay = 2000, multiplier = 3)
    )
    public void sendMessage(final String message, String caseId, final AtomicReference<Message> msg) {
        log.info("Tribs API - Sending message for caseId {}", caseId);

        try {
            jmsTemplate.convertAndSend(destination, message, m -> {
                msg.set(m);
                return m;
            });

            log.info("Message sent with message id {} for caseId {}", msg.get().getJMSMessageID(), caseId);

        } catch (IllegalStateException e) {
            if (connectionFactory instanceof CachingConnectionFactory) {
                log.info("Send failed for caseId {}, attempting to reset connection...", caseId);
                ((CachingConnectionFactory) connectionFactory).resetConnection();
                log.info("Resending..");
                jmsTemplate.send(destination, (Session session) -> session.createTextMessage(message));
                log.info("In catch, message sent for caseId {}, messageId unknown", caseId);
            } else {
                log.error("In catch, send message failed for caseId {} with exception: {}", caseId, e);
                throw e;
            }
        } catch (JMSException e) {
            log.info("Failed retrieving Message Id for caseId {}", caseId);
        }
    }

    @Recover
    public void recoverMessage(Throwable ex) throws Throwable {
        log.error("Tribs API - TopicPublisher.recover(): Send message failed with exception: ", ex);
        throw ex;
    }
}
