package uk.gov.hmcts.reform.sscs.jobscheduler.services;

import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;

public interface JobScheduler {

    <T> String schedule(Job<T> job);
}
