package uk.gov.hmcts.reform.sscs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.EventTelemetry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.model.Message;

@Slf4j
@Service
public class AppInsightsService {

    private final TelemetryClient client = new TelemetryClient();
    private final ObjectMapper om;

    public AppInsightsService(ObjectMapper om) {
        this.om = om;
    }

    public void sendAppInsightsEvent(Message message) throws JsonProcessingException {
        String serialisedMessage = messageToJson(message);

        client.trackEvent(new EventTelemetry(serialisedMessage));
        log.info("Event {} sent to AppInsights for Case ID {}", message, message.getCaseID());
    }

    public void sendAppInsightsEvent(String message) {
        client.trackEvent(new EventTelemetry(message));
        log.info("Event {} sent to AppInsights.", message);
    }

    private String messageToJson(Message message) throws JsonProcessingException {
        om.findAndRegisterModules();

        try {
            return om.writeValueAsString(message);
        } catch (JsonProcessingException jpe) {
            log.error("HMC failure message JsonProcessingException for Case ID: {}", message.getCaseID());
            throw jpe;
        }
    }
}
