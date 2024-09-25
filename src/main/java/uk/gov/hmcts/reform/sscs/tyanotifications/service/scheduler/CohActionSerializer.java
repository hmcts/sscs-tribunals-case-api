package uk.gov.hmcts.reform.sscs.tyanotifications.service.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobPayloadSerializer;

@Component
public class CohActionSerializer implements JobPayloadSerializer<CohJobPayload> {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String serialize(CohJobPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Cannot serialize CohJobPayload for case id "
                + "[" + payload.getCaseId() + "] online hearing id [" + payload.getOnlineHearingId() + "]");
        }
    }
}
