package uk.gov.hmcts.reform.sscs.service.servicebus;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@Service
// TODO: ASB - rename and move
public class TopicPublisher {

    @Autowired
    TopicConsumer topicConsumer;

    @Async
    public void sendMessage(Callback<SscsCaseData> message) {
        topicConsumer.onMessage(message);
    }

}
