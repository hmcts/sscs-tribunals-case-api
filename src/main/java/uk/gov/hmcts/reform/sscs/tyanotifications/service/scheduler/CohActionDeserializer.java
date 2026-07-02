package uk.gov.hmcts.reform.sscs.tyanotifications.service.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobPayloadDeserializer;

@Component
@RequiredArgsConstructor
public class CohActionDeserializer implements JobPayloadDeserializer<CohJobPayload> {

    private final ObjectMapper objectMapper;

    @Override
    public CohJobPayload deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, CohJobPayload.class);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot deserialize payload as CohJobPayload [" + payload + "]", e);
        }
    }
}
