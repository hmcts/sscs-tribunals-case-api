package uk.gov.hmcts.reform.sscs.config;

import javax.jms.ConnectionFactory;
import javax.jms.Session;
import lombok.extern.slf4j.Slf4j;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

@Slf4j
@Configuration
@EnableJms
@ConditionalOnProperty(name = "jms.enabled", havingValue = "true")
public class JMSConfiguration {

    @Value("${jms.namespace}")
    private String namespace;

    @Value("${jms.username}")
    private String username;

    @Value("${jms.password}")
    private String password;

    @Value("${jms.receiveTimeout}")
    private Long receiveTimeout;

    @Value("${jms.idleTimeout}")
    private Long idleTimeout;

    @Value("${jms.amqp-connection-string-template}")
    public String AMQP_CONNECTION_STRING_TEMPLATE;

    @Bean
    public ConnectionFactory tribunalsHearingsJmsConnectionFactory(@Value("${spring.application.name}") final String clientId) {
        String connection = String.format(AMQP_CONNECTION_STRING_TEMPLATE, namespace, idleTimeout);
        JmsConnectionFactory jmsConnectionFactory = new JmsConnectionFactory(connection);
        jmsConnectionFactory.setUsername(username);
        jmsConnectionFactory.setPassword(password);
        jmsConnectionFactory.setClientID(clientId);
        return new CachingConnectionFactory(jmsConnectionFactory);
    }

    @Bean
    public JmsListenerContainerFactory<DefaultMessageListenerContainer> tribunalsHearingsEventQueueContainerFactory(
        ConnectionFactory tribunalsHearingsJmsConnectionFactory,
        DefaultJmsListenerContainerFactoryConfigurer defaultJmsListenerContainerFactoryConfigurer) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        factory.setConnectionFactory(tribunalsHearingsJmsConnectionFactory);
        factory.setReceiveTimeout(receiveTimeout);
        factory.setSessionTransacted(Boolean.TRUE);
        factory.setSessionAcknowledgeMode(Session.SESSION_TRANSACTED);
        factory.setMessageConverter(new JsonMessageConverter());
        defaultJmsListenerContainerFactoryConfigurer.configure(factory, tribunalsHearingsJmsConnectionFactory);
        return factory;
    }

}
