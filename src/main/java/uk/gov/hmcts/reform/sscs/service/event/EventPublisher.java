package uk.gov.hmcts.reform.sscs.service.event;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.servicebus.TopicPublisher;

@Service
@Slf4j
@AllArgsConstructor
public class EventPublisher {

    private final TopicPublisher topicPublisher;

    public void publishEvent(Callback<SscsCaseData> callback) {
        log.info("Publishing message for the event {}", callback.getEvent());
        topicPublisher.sendMessage(callback);
    }

}
