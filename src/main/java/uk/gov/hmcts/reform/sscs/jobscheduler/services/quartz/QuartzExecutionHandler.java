package uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz;

import java.time.Instant;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.PersistJobDataAfterExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.JobDataKeys;

@Component
@DisallowConcurrentExecution
@PersistJobDataAfterExecution
public class QuartzExecutionHandler implements Job {

    private static final Logger log = LoggerFactory.getLogger(QuartzExecutionHandler.class);

    private final JobMapper jobMapper;

    public QuartzExecutionHandler(JobMapper jobMapper) {
        this.jobMapper = jobMapper;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {

        JobDetail jobDetail = context.getJobDetail();
        String jobId = jobDetail.getKey().getName();
        String jobGroup = jobDetail.getKey().getGroup();
        String jobName = jobDetail.getDescription();

        log.info("Executing job {}", jobId);

        try {

            Instant jobStart = Instant.now();

            String payloadSource = "";

            if (jobDetail.getJobDataMap().containsKey(JobDataKeys.PAYLOAD)) {

                payloadSource =
                    jobDetail
                        .getJobDataMap()
                        .getString(JobDataKeys.PAYLOAD);
            }

            JobMapping jobMapping = jobMapper.getJobMapping(payloadSource);
            jobMapping.execute(jobId, jobGroup, jobName, payloadSource);

            log.info(
                "Job {} executed in {}ms.",
                jobId, (Instant.now().toEpochMilli() - jobStart.toEpochMilli())
            );

        } catch (Exception e) {

            String errorMessage = String.format("Job failed. Job ID: %s", jobId);
            log.error(errorMessage, e);

            throw new JobExecutionException(errorMessage, e);
        }
    }

}
