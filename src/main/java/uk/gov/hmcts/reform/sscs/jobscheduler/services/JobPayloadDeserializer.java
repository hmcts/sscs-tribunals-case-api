package uk.gov.hmcts.reform.sscs.jobscheduler.services;

public interface JobPayloadDeserializer<T> {

    T deserialize(String payload);
}
