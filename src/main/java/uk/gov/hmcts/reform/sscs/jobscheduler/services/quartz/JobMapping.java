package uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz;

import java.util.function.Predicate;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobExecutor;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobPayloadDeserializer;

public class JobMapping<T> {
    private final Predicate<String> payloadCanBeHandled;
    private final JobPayloadDeserializer<T> jobPayloadDeserializer;
    private final JobExecutor<T> jobExecutor;

    public JobMapping(Predicate<String> payloadCanBeHandled,
                      JobPayloadDeserializer<T> jobPayloadDeserializer,
                      JobExecutor<T> jobExecutor) {
        this.payloadCanBeHandled = payloadCanBeHandled;
        this.jobPayloadDeserializer = jobPayloadDeserializer;
        this.jobExecutor = jobExecutor;
    }

    public boolean canHandle(String payload) {
        return payloadCanBeHandled.test(payload);
    }

    public void execute(String jobId, String jobGroup, String jobName, String payloadSource) {
        T payload = jobPayloadDeserializer.deserialize(payloadSource);
        jobExecutor.execute(jobId, jobGroup, jobName, payload);
    }
}
