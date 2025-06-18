package uk.gov.hmcts.reform.sscs.jobscheduler.model;

/**
 * Keys that job-scheduler stores in job's JobDataMap.
 */
public final class JobDataKeys {

    public static final String ATTEMPT = "attempt";
    public static final String PAYLOAD = "payload";

    private JobDataKeys() {
        // hiding default constructor
    }

}
