package uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.ZonedDateTime;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.JobDataKeys;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobException;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class QuartzJobSchedulerTest {

    private final Scheduler scheduler = mock(Scheduler.class);
    private final JobClassMapper jobClassMapper = mock(JobClassMapper.class);
    private final QuartzJobScheduler quartzJobScheduler = new QuartzJobScheduler(
        scheduler, jobClassMapper
    );
    private final JobClassMapping jobClassMapping = mock(JobClassMapping.class);

    @Test
    public void job_is_scheduled() {

        assertThatCode(
            () -> {

                String jobGroup = "test-job-group";
                String jobName = "test-job-name";
                String jobPayload = "payload";
                ZonedDateTime triggerAt = ZonedDateTime.now();

                Job<String> job = new Job<>(
                    jobGroup,
                    jobName,
                    jobPayload,
                    triggerAt
                );

                when(jobClassMapper.getJobMapping(String.class)).thenReturn(jobClassMapping);
                when(jobClassMapping.serialize(jobPayload)).thenReturn("serialized-payload");

                String actualJobId = quartzJobScheduler.schedule(job);

                ArgumentCaptor<JobDetail> jobDetailCaptor = ArgumentCaptor.forClass(JobDetail.class);
                ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);

                verify(scheduler, times(1)).scheduleJob(
                    jobDetailCaptor.capture(),
                    triggerCaptor.capture()
                );

                JobDetail actualJobDetail = jobDetailCaptor.getValue();
                assertEquals(actualJobId, actualJobDetail.getKey().getName());
                assertEquals(jobGroup, actualJobDetail.getKey().getGroup());
                assertEquals(jobName, actualJobDetail.getDescription());
                assertTrue(actualJobDetail.getJobDataMap().containsKey(JobDataKeys.PAYLOAD));
                assertEquals("serialized-payload", actualJobDetail.getJobDataMap().get(JobDataKeys.PAYLOAD));

                Trigger actualTrigger = triggerCaptor.getValue();
                assertEquals(actualTrigger.getStartTime().toInstant().toEpochMilli(), triggerAt.toInstant().toEpochMilli());
            }
        ).doesNotThrowAnyException();
    }

    @Test
    public void schedule_wraps_exception_from_client_deserializer() {

        assertThatThrownBy(
            () -> {

                String jobGroup = "test-job-group";
                String jobName = "test-job";
                String jobPayload = "payload";
                ZonedDateTime triggerAt = ZonedDateTime.now();

                Job<String> job = new Job<>(
                    jobGroup,
                    jobName,
                    jobPayload,
                    triggerAt
                );

                when(jobClassMapper.getJobMapping(String.class)).thenReturn(jobClassMapping);
                doThrow(RuntimeException.class)
                    .when(jobClassMapping)
                    .serialize(jobPayload);

                quartzJobScheduler.schedule(job);
            }
        ).hasMessage("Error while scheduling job")
            .isExactlyInstanceOf(JobException.class);
    }

    @Test
    public void schedule_throws_when_quartz_fails() {

        assertThatThrownBy(
            () -> {

                String jobGroup = "test-job-group";
                String jobName = "test-job";
                String jobPayload = "payload";
                ZonedDateTime triggerAt = ZonedDateTime.now();

                when(jobClassMapper.getJobMapping(String.class)).thenReturn(jobClassMapping);
                when(jobClassMapping.serialize(jobPayload)).thenReturn("serialized-payload");

                doThrow(RuntimeException.class)
                    .when(scheduler)
                    .scheduleJob(any(), any());

                Job<String> job = new Job<>(jobGroup, jobName, jobPayload, triggerAt);
                quartzJobScheduler.schedule(job);
            }
        ).hasMessage("Error while scheduling job")
            .isExactlyInstanceOf(JobException.class);
    }
}
