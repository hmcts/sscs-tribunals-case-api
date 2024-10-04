package uk.gov.hmcts.reform.sscs.tyanotifications.service.scheduler;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobPayloadDeserializer;

@Component
public class CcdActionDeserializer implements JobPayloadDeserializer<String> {

    @Override
    public String deserialize(String payload) {
        return payload;
    }
}
