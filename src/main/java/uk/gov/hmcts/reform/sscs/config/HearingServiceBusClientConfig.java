package uk.gov.hmcts.reform.sscs.config;


import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("${feature.gaps-switchover.enabled} && !${jms.enabled}")
public class HearingServiceBusClientConfig {

    @Value("${azure.servicebus.hearings.connection-string}")
    private String connectionString;

    @Value("${azure.servicebus.hearings.destination}")
    private String queueName;

    @Bean
    public ServiceBusSenderClient hearingServiceBusClient() {
        return new ServiceBusClientBuilder()
            .connectionString(connectionString)
            .sender()
            .queueName(queueName)
            .buildClient();
    }
}
