package uk.gov.hmcts.reform.sscs.service.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.exception.OrchestratorJsonException;
import uk.gov.hmcts.reform.sscs.service.servicebus.TopicPublisher;

@Service
@Slf4j
@AllArgsConstructor
public class EventPublisher {

    private final TopicPublisher topicPublisher;
    private final ObjectMapper objectMapper;

    public void publishEvent(Callback<SscsCaseData> callback) {
        log.info("Publishing message for the event {}", callback.getEvent());
        String body = buildCaseDataMap(callback);
        String caseId = String.valueOf(callback.getCaseDetails().getId());
        topicPublisher.sendMessage(body, caseId, new AtomicReference<>());
    }

    private String buildCaseDataMap(final Callback<SscsCaseData> callback) {
        try {
            return objectMapper.writeValueAsString(callback);
        } catch (IOException e) {
            throw new OrchestratorJsonException(e);
        }
    }
}
