package uk.gov.hmcts.reform.sscs.config;

import javax.jms.ConnectionFactory;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.policy.JmsDefaultPrefetchPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

@Configuration
@Slf4j
@EnableJms
public class MessagingConfig {

    @Bean
    public String jmsUrlString(@Value("${amqp.amqp-connection-string-template}") final String amqpConnectionStringTemplate,
                               @Value("${amqp.idleTimeout}") final Long idleTimeout,
                               @Value("${amqp.host}") final String host) {
        return String.format(amqpConnectionStringTemplate, host, idleTimeout);
    }

    @Bean
    public ConnectionFactory jmsConnectionFactory(@Value("${spring.application.name}") final String clientId,
                                                  @Value("${amqp.username}") final String username,
                                                  @Value("${amqp.password}") final String password,
                                                  @Autowired final String jmsUrlString,
                                                  @Autowired(required = false) final SSLContext jmsSslContext,
                                                  @Value("${amqp.prefetch.override}") final boolean prefetchOverride,
                                                  @Value("${amqp.prefetch.topicPrefetch}") final int topicPrefetch) {
        JmsConnectionFactory jmsConnectionFactory = new JmsConnectionFactory(jmsUrlString);
        jmsConnectionFactory.setUsername(username);
        jmsConnectionFactory.setPassword(password);
        jmsConnectionFactory.setClientID(clientId);
        jmsConnectionFactory.setReceiveLocalOnly(true);
        if (jmsSslContext != null) {
            jmsConnectionFactory.setSslContext(jmsSslContext);
        }
        if (prefetchOverride) {
            JmsDefaultPrefetchPolicy prefetchPolicy = new JmsDefaultPrefetchPolicy();
            prefetchPolicy.setTopicPrefetch(topicPrefetch);
            prefetchPolicy.setDurableTopicPrefetch(topicPrefetch);
            jmsConnectionFactory.setPrefetchPolicy(prefetchPolicy);
        }
        return new CachingConnectionFactory(jmsConnectionFactory);
    }

    @Bean
    public JmsTemplate jmsTemplate(ConnectionFactory jmsConnectionFactory) {
        JmsTemplate returnValue = new JmsTemplate();
        returnValue.setConnectionFactory(jmsConnectionFactory);
        return returnValue;
    }

    @Bean
    public JmsListenerContainerFactory topicJmsListenerContainerFactory(ConnectionFactory connectionFactory) {
        log.info("Creating JMSListenerContainer bean for topics..");
        DefaultJmsListenerContainerFactory returnValue = new DefaultJmsListenerContainerFactory();
        returnValue.setConnectionFactory(connectionFactory);
        returnValue.setSubscriptionDurable(Boolean.TRUE);
        returnValue.setErrorHandler(t -> log.error("Error while processing JMS message", t));
        returnValue.setExceptionListener(t -> log.error("Exception while processing JMS message", t));
        return returnValue;
    }
}
