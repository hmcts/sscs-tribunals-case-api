package uk.gov.hmcts.reform.sscs.service.servicebus.messaging;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class MessagingConfigTest {

    private final MessagingConfig messagingConfig = new MessagingConfig();

    @Test
    public void jmsUrlStringFormatsTheAmqpString() {
        final String url = messagingConfig.jmsUrlString("myHost");
        assertTrue("Jms url string should begin with amqps://<host> ", url.startsWith("amqps://myHost?"));
    }

}
