package uk.gov.hmcts.reform.sscs.config;

import jakarta.jms.ConnectionFactory;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.policy.JmsDefaultPrefetchPolicy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

@Configuration
@Slf4j
@EnableJms
public class MessagingConfig {

    @Bean("evidenceShareJmsUrl")
    public String jmsUrlString(@Value("${amqp.amqp-connection-string-template}") final String amqpConnectionStringTemplate,
                               @Value("${amqp.idleTimeout}") final Long idleTimeout,
                               @Value("${amqp.host}") final String host) {
        return String.format(amqpConnectionStringTemplate, host, idleTimeout);
    }

    @Bean("evidenceShareJmsConnectionFactory")
    public ConnectionFactory jmsConnectionFactory(@Value("${spring.application.name}") final String clientId,
                                                  @Value("${amqp.username}") final String username,
                                                  @Value("${amqp.password}") final String password,
                                                  @Autowired @Qualifier("evidenceShareJmsUrl") final String jmsUrlString,
                                                  @Autowired(required = false) final SSLContext jmsSslContext,
                                                  @Value("${amqp.prefetch.override}") final boolean prefetchOverride,
                                                  @Value("${amqp.prefetch.topicPrefetch}") final int topicPrefetch,
                                                  @Value("${amqp.reconnectOnException}") final boolean reconnectOnException
                                                  ) {
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
        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(jmsConnectionFactory);
        if (reconnectOnException) {
            cachingConnectionFactory.setReconnectOnException(true);
        }
        var factory = new CachingConnectionFactory(jmsConnectionFactory);
        factory.setCacheConsumers(false);
        factory.setCacheProducers(false);
        return factory;
    }

    @Bean("evidenceShareJmsTemplate")
    public JmsTemplate jmsTemplate(@Qualifier("evidenceShareJmsConnectionFactory") ConnectionFactory jmsConnectionFactory) {
        JmsTemplate returnValue = new JmsTemplate();
        returnValue.setConnectionFactory(jmsConnectionFactory);
        return returnValue;
    }

    @Bean("evidenceShareJmsListenerContainerFactory")
    public JmsListenerContainerFactory<DefaultMessageListenerContainer> topicJmsListenerContainerFactory(@Qualifier("evidenceShareJmsConnectionFactory") ConnectionFactory connectionFactory) {
        log.info("Creating JMSListenerContainer bean for topics..");
        DefaultJmsListenerContainerFactory returnValue = new DefaultJmsListenerContainerFactory();
        returnValue.setConnectionFactory(connectionFactory);
        returnValue.setSubscriptionDurable(Boolean.TRUE);
        returnValue.setErrorHandler(t -> log.error("Error while processing JMS message", t));
        returnValue.setExceptionListener(t -> log.error("Exception while processing JMS message", t));
        return returnValue;
    }
}
