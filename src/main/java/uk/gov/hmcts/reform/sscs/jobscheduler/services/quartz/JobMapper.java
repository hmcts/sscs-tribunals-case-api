package uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz;

import java.util.List;

public class JobMapper {
    private final List<JobMapping> jobMappings;

    public JobMapper(List<JobMapping> jobMappings) {
        this.jobMappings = jobMappings;
    }

    public JobMapping getJobMapping(String payload) {
        return jobMappings.stream()
                .filter(jobMapping -> jobMapping.canHandle(payload))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cannot map payload [" + payload + "]"));
    }
}
