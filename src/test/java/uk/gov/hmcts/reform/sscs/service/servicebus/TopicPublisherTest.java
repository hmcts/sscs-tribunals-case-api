package uk.gov.hmcts.reform.sscs.service.servicebus;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(MockitoJUnitRunner.class)
public class TopicPublisherTest {

    @Mock
    private TopicConsumer topicConsumer;

    @Mock
    private Callback<SscsCaseData> callback;

    @InjectMocks
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

}
