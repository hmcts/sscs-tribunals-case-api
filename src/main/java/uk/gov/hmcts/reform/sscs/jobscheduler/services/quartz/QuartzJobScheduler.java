package uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

import java.time.ZonedDateTime;
import java.util.Date;
import java.util.UUID;
import org.quartz.Scheduler;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.JobDataKeys;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobException;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;

@Service
public class QuartzJobScheduler implements JobScheduler {

    private final Scheduler scheduler;

    private final JobClassMapper jobClassMapper;

    public QuartzJobScheduler(Scheduler scheduler, JobClassMapper jobClassMapper) {
        this.scheduler = scheduler;
        this.jobClassMapper = jobClassMapper;
    }

    public <T> String schedule(Job<T> job) {
        try {
            String jobId = UUID.randomUUID().toString();

            Class<T> payloadClass = (Class<T>) job.payload.getClass();
            JobClassMapping<T> jobMapping = jobClassMapper.getJobMapping(payloadClass);

            scheduler.scheduleJob(
                newJob(QuartzExecutionHandler.class)
                    .withIdentity(jobId, job.group)
                    .withDescription(job.name)
                    .usingJobData(JobDataKeys.PAYLOAD, jobMapping.serialize(job.payload))
                    .requestRecovery()
                    .build(),
                toQuartzTrigger(job.triggerAt)
            );

            return jobId;

        } catch (Exception e) {
            throw new JobException("Error while scheduling job", e);
        }
    }

    private static org.quartz.Trigger toQuartzTrigger(ZonedDateTime triggerDateTime) {
        return newTrigger()
            .startAt(Date.from(triggerDateTime.toInstant()))
            .usingJobData(JobDataKeys.ATTEMPT, 1)
            .build();
    }
}
