package uk.gov.hmcts.reform.sscs.healthcheck;
import com.azure.core.util.BinaryData;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusException;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.stubbing.BaseStubbing;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.actuate.health.Health;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EvidenceShareTopicConsumerHealthIndicatorTest {

    @InjectMocks
    private EvidenceShareTopicConsumerHealthIndicator EvidenceShareTopicConsumerHealthIndicator;

    @Mock
    private ServiceBusReceiverClient serviceBusReceiverClient;

    @Mock
    private ServiceBusReceivedMessage serviceBusReceivedMessage;

//    public static class ServiceBusModelFactory;

    @Before
    public void setUp() {

        ReflectionTestUtils.setField(EvidenceShareTopicConsumerHealthIndicator, "topicName", "sscs-evidenceshare-topic-aat");
        ReflectionTestUtils.setField(EvidenceShareTopicConsumerHealthIndicator, "subscriptionName", "sscs-evidenceshare-subscription-aat");
        ReflectionTestUtils.setField(EvidenceShareTopicConsumerHealthIndicator, "connectionString",
                "Endpoint=Endpoint;SharedAccessKeyName=SharedAccessKeyName;SharedAccessKey=SharedAccessKey");
    }

    @Test
    public void should_report_as_up_if_peekMessage_succeeds() {

        ReflectionTestUtils.setField(serviceBusReceiverClient, "peekMessage", serviceBusReceivedMessage);

//        Mockito.when(ServiceBusReceiverClient.peekMessage()).thenReturn(serviceBusReceivedMessage);

        assertEquals(Health.up().build(), EvidenceShareTopicConsumerHealthIndicator.health());
    }

    @Test
    public void should_report_as_down_if_peekMessage_fails() throws ServiceBusException {

        assertEquals(Health.down().build(), EvidenceShareTopicConsumerHealthIndicator.health());
    }


//    private ServiceBusReceiverClient getReceiver() {
//
//        return new ServiceBusClientBuilder()
//                .connectionString("connectionString")
//                .receiver()
//                .maxAutoLockRenewDuration(Duration.ofMinutes(1))
//                .topicName("")
//                .subscriptionName("subsriptionName")
//                .buildClient();
//    }
}
