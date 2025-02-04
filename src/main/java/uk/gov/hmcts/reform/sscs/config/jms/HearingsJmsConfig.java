package uk.gov.hmcts.reform.sscs.config.jms;

import jakarta.jms.ConnectionFactory;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

@Slf4j
@Configuration
public class HearingsJmsConfig {

    public static final String AMQP_CONNECTION_STRING_TEMPLATE = "amqps://%1s?amqp.idleTimeout=%2d";
    @Value("${azure.service-bus.hmc-to-hearings-api.namespace}")
    private String namespace;

    @Value("${azure.service-bus.connection-postfix}")
    private String connectionPostfix;

    @Value("${azure.service-bus.hmc-to-hearings-api.username}")
    private String username;

    @Value("${azure.service-bus.hmc-to-hearings-api.password}")
    private String password;

    @Value("${azure.service-bus.hmc-to-hearings-api.receiveTimeout}")
    private Long receiveTimeout;

    @Value("${azure.service-bus.hmc-to-hearings-api.idleTimeout}")
    private Long idleTimeout;

    @Value("${spring.application.name}")
    private String clientId;

    @Bean("hmcHearingsJmsUrl")
    public String jmsUrlString(@Value("${azure.service-bus.hmc-to-hearings-api.namespace}${azure.service-bus.connection-postfix}") final String host) {
        return String.format(AMQP_CONNECTION_STRING_TEMPLATE, host, idleTimeout);
    }

    @Bean("hmcHearingsJmsConnectionFactory")
    public ConnectionFactory jmsConnectionFactory(@Autowired @Qualifier("hmcHearingsJmsUrl") final String jmsUrlString,
                                                  @Autowired(required = false) final SSLContext jmsSslContext
    ) {
        JmsConnectionFactory jmsConnectionFactory = new JmsConnectionFactory(jmsUrlString);
        jmsConnectionFactory.setUsername(username);
        jmsConnectionFactory.setPassword(password);
        jmsConnectionFactory.setClientID(clientId);
        jmsConnectionFactory.setReceiveLocalOnly(true);
        if (jmsSslContext != null) {
            jmsConnectionFactory.setSslContext(jmsSslContext);
        }
        var factory = new CachingConnectionFactory(jmsConnectionFactory);
        factory.setCacheConsumers(false);
        factory.setCacheProducers(false);
        return factory;
    }

    @Bean("hmcHearingsJmsTemplate")
    public JmsTemplate jmsTemplate(@Qualifier("hmcHearingsJmsConnectionFactory") ConnectionFactory jmsConnectionFactory) {
        JmsTemplate returnValue = new JmsTemplate();
        returnValue.setConnectionFactory(jmsConnectionFactory);
        return returnValue;
    }

    @Bean("hmcHearingsEventTopicContainerFactory")
    @ConditionalOnProperty("flags.hmc-to-hearings-api.enabled")
    public JmsListenerContainerFactory<DefaultMessageListenerContainer> hmcHearingsEventTopicContainerFactory(@Qualifier("hmcHearingsJmsConnectionFactory") ConnectionFactory connectionFactory) {
        log.info("Creating JMSListenerContainer bean for topics..");
        DefaultJmsListenerContainerFactory returnValue = new DefaultJmsListenerContainerFactory();
        returnValue.setConnectionFactory(connectionFactory);
        returnValue.setSubscriptionDurable(Boolean.TRUE);
        returnValue.setErrorHandler(t -> log.error("Error while processing JMS message", t));
        returnValue.setExceptionListener(t -> log.error("Exception while processing JMS message", t));
        return returnValue;
    }
}
