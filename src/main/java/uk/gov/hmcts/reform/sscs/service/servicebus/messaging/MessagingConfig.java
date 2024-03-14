package uk.gov.hmcts.reform.sscs.service.servicebus.messaging;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.jms.ConnectionFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.config.JmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@Slf4j
public class MessagingConfig {

    @Value("${amqp.amqp-connection-string-template}")
    private String amqpConnectionStringTemplate;

    @Value("${amqp.idleTimeout}")
    private Long idleTimeout;

    @Bean
    public String jmsUrlString(@Value("${amqp.host}") final String host) {
        return String.format(amqpConnectionStringTemplate, host, idleTimeout);
    }

    @Bean
    public ConnectionFactory jmsConnectionFactory(@Value("${amqp.clientId}") final String clientId,
                                                  @Value("${amqp.username}") final String username,
                                                  @Value("${amqp.password}") final String password,
                                                  @Autowired final String jmsUrlString,
                                                  @Autowired(required = false) final SSLContext jmsSslContext) {
        JmsConnectionFactory jmsConnectionFactory = new JmsConnectionFactory(jmsUrlString);
        jmsConnectionFactory.setUsername(username);
        jmsConnectionFactory.setPassword(password);
        jmsConnectionFactory.setClientID(clientId);
        jmsConnectionFactory.setReceiveLocalOnly(true);
        if (jmsSslContext != null) {
            jmsConnectionFactory.setSslContext(jmsSslContext);
        }

        return new CachingConnectionFactory(jmsConnectionFactory);
    }

    @Bean
    public SSLContext jmsSslContext(@Value("${amqp.trustAllCerts}") final boolean trustAllCerts)
        throws NoSuchAlgorithmException, KeyManagementException {

        if (trustAllCerts) {
            // https://stackoverflow.com/a/2893932
            // DO NOT USE THIS IN PRODUCTION!
            TrustManager[] trustCerts = getTrustManagers();

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustCerts, new SecureRandom());

            return sc;
        }
        return null;
    }

    /*
     * DO NOT USE THIS IN PRODUCTION!
     * This was only used for testing unverified ssl certs locally!
     */
    @Deprecated
    private TrustManager[] getTrustManagers() {
        return new TrustManager[]{
            new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }

                @Override
                public void checkClientTrusted(
                    X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(
                    X509Certificate[] certs, String authType) {
                }
            }
        };
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
        returnValue.setBackOff(new FixedBackOff(5000, 3));
        returnValue.setErrorHandler(new JmsErrorHandler());
        return returnValue;
    }

}
