package uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz;

import static org.quartz.TriggerBuilder.newTrigger;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.JobDataKeys;

/**
 * Listens for failed jobs and schedules their re-execution.
 */
public class QuartzFailedJobRescheduler implements JobListener {

    private static final Logger log = LoggerFactory.getLogger(QuartzFailedJobRescheduler.class);

    private final int maxNumberOfAttempts;
    private final Duration delayBetweenAttempts;

    public QuartzFailedJobRescheduler(
        int maxNumberOfAttempts,
        Duration delayBetweenAttempts
    ) {
        this.maxNumberOfAttempts = maxNumberOfAttempts;
        this.delayBetweenAttempts = delayBetweenAttempts;
    }

    @Override
    public String getName() {
        return "Failed Job Rescheduler";
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        // nothing to do
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        // nothing to do
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        if (jobException != null) {
            try {
                handleFailedJobExecution(context);
            } catch (Exception e) {
                log.error(
                    "Failed to schedule the re-execution of a previously failed job. Job ID: {}",
                    context.getJobDetail().getKey().getName(),
                    e
                );
            }
        }
    }

    private void handleFailedJobExecution(JobExecutionContext context) throws SchedulerException {
        String jobId = context.getJobDetail().getKey().getName();
        int lastAttemptNumber = (Integer)context.getMergedJobDataMap().get(JobDataKeys.ATTEMPT);

        if (lastAttemptNumber < maxNumberOfAttempts) {
            log.error("Job execution failed on attempt {}. ID: {}", lastAttemptNumber, jobId);
            scheduleAnotherExecutionAttempt(context, lastAttemptNumber + 1);
        } else {
            log.error(
                "Failed to successfully execute job (ID: {}) in {} attempts",
                jobId,
                lastAttemptNumber
            );
        }
    }

    private void scheduleAnotherExecutionAttempt(
        JobExecutionContext lastExecutionContext,
        int executionAttemptNumber
    ) throws SchedulerException {

        lastExecutionContext.getScheduler().scheduleJob(
            newTrigger()
                .forJob(lastExecutionContext.getJobDetail())
                .usingJobData(lastExecutionContext.getMergedJobDataMap())
                .usingJobData(JobDataKeys.ATTEMPT, executionAttemptNumber)
                .startAt(Date.from(Instant.now().plus(delayBetweenAttempts)))
                .build()
        );

        log.info(
            "Scheduled re-execution of a failed job (ID: {}). Attempt {}.",
            lastExecutionContext.getJobDetail().getKey().getName(),
            executionAttemptNumber
        );
    }

}
