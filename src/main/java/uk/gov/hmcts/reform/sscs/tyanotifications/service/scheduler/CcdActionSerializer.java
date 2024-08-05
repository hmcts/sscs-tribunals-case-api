package uk.gov.hmcts.reform.sscs.tyanotifications.service.scheduler;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobPayloadSerializer;

@Component
public class CcdActionSerializer implements JobPayloadSerializer<String> {

    @Override
    public String serialize(String payload) {
        return payload;
    }
}
