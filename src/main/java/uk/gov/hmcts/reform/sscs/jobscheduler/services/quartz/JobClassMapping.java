package uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz;

import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobPayloadSerializer;

public class JobClassMapping<T> {
    private final Class<T> canHandlePayloadClass;
    private final JobPayloadSerializer<T> jobPayloadSerializer;

    public JobClassMapping(Class<T> canHandlePayloadClass,
                           JobPayloadSerializer<T> jobPayloadSerializer) {
        this.canHandlePayloadClass = canHandlePayloadClass;
        this.jobPayloadSerializer = jobPayloadSerializer;
    }

    public boolean canHandle(Class payload) {
        return canHandlePayloadClass.equals(payload);
    }

    public String serialize(T payload) {
        return jobPayloadSerializer.serialize(payload);
    }
}
