package uk.gov.hmcts.reform.sscs.service.servicebus;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TopicPublisher.class)
@EnableRetry
// TODO: ASB - is this now an integration test? It's kind of pointless anyway, as it's just testing the retryable annotation
public class TopicPublisherTest {

    @MockitoBean
    private TopicConsumer topicConsumer;

    @Mock
    private Callback<SscsCaseData> callback;

    @Autowired
    private TopicPublisher topicPublisher;

    @Test
    public void sendMessage_callsTopicConsumerOnMessage() {
        topicPublisher.sendMessage(callback);
        verify(topicConsumer, times(1)).onMessage(callback);
    }

    @Test(expected = IllegalStateException.class)
    public void sendMessage_retriesOnException() {
        doThrow(new IllegalStateException()).when(topicConsumer).onMessage(callback);
        topicPublisher.sendMessage(callback);
    }

    @Test
    public void sendMessage_succeedsAfterRetry() {
        doThrow(new IllegalStateException()).doNothing().when(topicConsumer).onMessage(callback);
        topicPublisher.sendMessage(callback);
        verify(topicConsumer, times(2)).onMessage(callback);
    }
}
