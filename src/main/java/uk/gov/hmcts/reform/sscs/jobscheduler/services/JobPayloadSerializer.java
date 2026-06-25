package uk.gov.hmcts.reform.sscs.jobscheduler.services;

public interface JobPayloadSerializer<T> {

    String serialize(T payload);
}
