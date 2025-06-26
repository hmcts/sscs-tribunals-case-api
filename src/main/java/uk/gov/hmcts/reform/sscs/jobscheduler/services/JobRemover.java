package uk.gov.hmcts.reform.sscs.jobscheduler.services;

public interface JobRemover {

    void remove(String jobId, String jobGroup);

    void removeGroup(String jobGroup);

}
