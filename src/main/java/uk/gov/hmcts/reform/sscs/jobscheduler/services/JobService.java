package uk.gov.hmcts.reform.sscs.jobscheduler.services;

public interface JobService {

    void start();

    void stop(boolean waitForJobsToComplete);
}
