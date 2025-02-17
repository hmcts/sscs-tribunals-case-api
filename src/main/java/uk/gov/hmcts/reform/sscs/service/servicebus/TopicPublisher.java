package uk.gov.hmcts.reform.sscs.service.servicebus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Service
public class TopicPublisher {

    @Autowired
    TopicConsumer topicConsumer;

    @Retryable(
        maxAttempts = 5,
        backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public void sendMessage(Callback<SscsCaseData> message) {
        topicConsumer.onMessage(message);
    }

}
