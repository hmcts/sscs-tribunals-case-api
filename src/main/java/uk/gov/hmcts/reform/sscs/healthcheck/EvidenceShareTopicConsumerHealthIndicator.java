package uk.gov.hmcts.reform.sscs.healthcheck;

import java.time.Duration;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

public class EvidenceShareTopicConsumerHealthIndicator implements HealthIndicator {

    @Value("${azure.servicebus.hearings.connection-string}")
    private String connectionString;

    @Value("${amqp.topic}")
    private String topicName;

    @Value("${amqp.subscription}")
    private String subscriptionName;

    public Health health() {
//        remove if statement once ready to release to prod
        if (topicName !="sscs-evidenceshare-topic-aat"){
            return new Health.Builder().up().build();
        }
        else {
            try {

                ServiceBusReceiverClient receiver = new ServiceBusClientBuilder()
                        .connectionString(connectionString)
                        .receiver()
                        .maxAutoLockRenewDuration(Duration.ofMinutes(1))
                        .topicName(topicName)
                        .subscriptionName(subscriptionName)
                        .buildClient();

                receiver.peekMessage();

                return new Health.Builder().up().build();

            } catch (ServiceBusException e) {
                LoggerFactory.getLogger(EvidenceShareTopicConsumerHealthIndicator.class)
                        .error("Error performing evidence share healthcheck", e);
                return new Health.Builder().down(e).build();
            }

        }
    }
}

