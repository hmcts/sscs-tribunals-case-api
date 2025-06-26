package uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.JobDataKeys;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class QuartzExecutionHandlerTest {

    private JobMapper jobMapper = mock(JobMapper.class);
    private final QuartzExecutionHandler quartzExecutionHandler = new QuartzExecutionHandler(jobMapper);

    @Test
    public void execute_deserializes_payload_and_delegates_execution() {

        assertThatCode(
            () -> {

                JobExecutionContext context = mock(JobExecutionContext.class);
                JobDetail jobDetail = mock(JobDetail.class);
                JobDataMap jobDataMap = mock(JobDataMap.class);

                when(context.getJobDetail()).thenReturn(jobDetail);
                when(jobDetail.getKey()).thenReturn(new JobKey("job-id", "job-group"));
                when(jobDetail.getDescription()).thenReturn("job-name");
                when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);

                when(jobDataMap.containsKey(JobDataKeys.PAYLOAD)).thenReturn(true);
                when(jobDataMap.getString(JobDataKeys.PAYLOAD)).thenReturn("payload-stuff");
                JobMapping jobMapping = mock(JobMapping.class);
                when(jobMapper.getJobMapping("payload-stuff")).thenReturn(jobMapping);

                quartzExecutionHandler.execute(context);

                verify(jobMapping, times(1)).execute(
                    eq("job-id"),
                    eq("job-group"),
                    eq("job-name"),
                    eq("payload-stuff")
                );
            }
        ).doesNotThrowAnyException();
    }

    @Test
    public void execute_uses_empty_payload_when_not_set() {

        assertThatCode(
            () -> {

                JobExecutionContext context = mock(JobExecutionContext.class);
                JobDetail jobDetail = mock(JobDetail.class);
                JobDataMap jobDataMap = mock(JobDataMap.class);

                when(context.getJobDetail()).thenReturn(jobDetail);
                when(jobDetail.getKey()).thenReturn(new JobKey("job-id", "job-group"));
                when(jobDetail.getDescription()).thenReturn("job-name");
                when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);

                when(jobDataMap.containsKey(JobDataKeys.PAYLOAD)).thenReturn(false);
                JobMapping jobMapping = mock(JobMapping.class);
                when(jobMapper.getJobMapping("")).thenReturn(jobMapping);

                quartzExecutionHandler.execute(context);

                verify(jobMapping, times(1)).execute(
                    eq("job-id"),
                    eq("job-group"),
                    eq("job-name"),
                    eq("")
                );
            }
        ).doesNotThrowAnyException();
    }

    @Test
    public void execute_wraps_exception_from_client_deserializer() {

        assertThatThrownBy(
            () -> {

                JobExecutionContext context = mock(JobExecutionContext.class);
                JobDetail jobDetail = mock(JobDetail.class);
                JobDataMap jobDataMap = mock(JobDataMap.class);

                when(context.getJobDetail()).thenReturn(jobDetail);
                when(jobDetail.getKey()).thenReturn(new JobKey("job-id", "job-group"));
                when(jobDetail.getDescription()).thenReturn("job-name");
                when(jobDetail.getJobDataMap()).thenReturn(jobDataMap);

                when(jobDataMap.containsKey(JobDataKeys.PAYLOAD)).thenReturn(false);

                quartzExecutionHandler.execute(context);
            }
        ).hasMessage("Job failed. Job ID: job-id");
    }
}
