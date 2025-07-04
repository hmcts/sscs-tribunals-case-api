package uk.gov.hmcts.reform.sscs.jobscheduler;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.time.ZonedDateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import uk.gov.hmcts.reform.sscs.jobscheduler.model.Job;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobExecutor;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobPayloadDeserializer;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobPayloadSerializer;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobRemover;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobScheduler;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobService;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz.JobClassMapper;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz.JobClassMapping;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz.JobMapper;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz.JobMapping;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = SpringBootContextRoot.class)
@ActiveProfiles("integration")
public class ApplicationTest {

    @Autowired
    @Qualifier("scheduler")
    private Scheduler quartzScheduler;

    @Autowired
    private JobService jobService;

    @Autowired
    private JobScheduler jobScheduler;

    @Autowired
    private JobRemover jobRemover;

    @MockitoBean
    private IdamApi idamApi;

    @MockitoBean
    private JobPayloadSerializer<TestPayload> jobPayloadSerializer;

    @MockitoBean
    private JobPayloadDeserializer<TestPayload> jobPayloadDeserializer;

    @MockitoBean
    private JobExecutor<TestPayload> jobExecutor;

    @MockitoBean
    private JobClassMapper jobClassMapper;

    @MockitoBean
    private JobMapper jobMapper;

    TestPayload testPayload = new TestPayload();

    @Before
    public void setUp() {

        jobService.start();

        try {
            quartzScheduler.clear();
        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }

        given(jobPayloadSerializer.serialize(testPayload)).willReturn("serialized-payload");
        given(jobPayloadDeserializer.deserialize("serialized-payload")).willReturn(testPayload);

        given(jobClassMapper.getJobMapping(TestPayload.class)).willReturn(new JobClassMapping<>(TestPayload.class, jobPayloadSerializer));
        given(jobMapper.getJobMapping(any())).willReturn(new JobMapping<>(x -> true, jobPayloadDeserializer, jobExecutor));
    }

    @Test
    public void jobIsScheduledAndExecutesInTheFuture() {

        assertTrue("Job scheduler is empty at start", getScheduledJobCount() == 0);

        String jobGroup = "test-job-group";
        String jobName = "test-job-name";

        Job<TestPayload> job = new Job<>(
            jobGroup,
            jobName,
            testPayload,
            ZonedDateTime.now().plusSeconds(2)
        );

        String jobId = jobScheduler.schedule(job);

        assertNotNull(jobId);

        assertTrue("Job was scheduled into Quartz", getScheduledJobCount() == 1);

        // job is executed
        verify(jobExecutor, timeout(10000)).execute(
            eq(jobId),
            eq(jobGroup),
            eq(jobName),
            eq(testPayload)
        );
    }

    @Test
    public void jobIsScheduledAndThenRemovedByGroup() {

        assertTrue("Job scheduler is empty at start", getScheduledJobCount() == 0);

        String jobGroup = "test-job-group";
        String jobName = "test-job-name";

        Job<TestPayload> job1 = new Job<>(
            jobGroup,
            jobName,
            testPayload,
            ZonedDateTime.now().plusSeconds(2)
        );

        String jobId1 = jobScheduler.schedule(job1);

        assertNotNull(jobId1);

        Job<TestPayload> job2 = new Job<>(
            jobGroup,
            jobName,
            testPayload,
            ZonedDateTime.now().plusSeconds(2)
        );

        String jobId2 = jobScheduler.schedule(job2);

        assertNotNull(jobId2);

        assertTrue("Jobs were scheduled into Quartz", getScheduledJobCount() == 2);

        jobRemover.removeGroup(jobGroup);

        assertTrue("Jobs were removed from Quartz after execution", getScheduledJobCount() == 0);

        // jobs are /never/ executed
        verify(jobExecutor, after(10000).never()).execute(
            eq(jobId1),
            eq(jobGroup),
            eq(jobName),
            eq(testPayload)
        );

        verify(jobExecutor, after(10000).never()).execute(
            eq(jobId2),
            eq(jobGroup),
            eq(jobName),
            eq(testPayload)
        );
    }

    @Test
    public void jobIsScheduledAndThenRemovedById() {

        assertTrue("Job scheduler is empty at start", getScheduledJobCount() == 0);

        String jobGroup = "test-job-group";
        String jobName = "test-job-name";

        Job<TestPayload> job = new Job<>(
            jobGroup,
            jobName,
            testPayload,
            ZonedDateTime.now().plusSeconds(2)
        );

        String jobId = jobScheduler.schedule(job);

        assertNotNull(jobId);

        assertTrue("Job was scheduled into Quartz", getScheduledJobCount() == 1);

        jobRemover.remove(jobId, jobGroup);

        assertTrue("Job was removed from Quartz after execution", getScheduledJobCount() == 0);

        // job is /never/ executed
        verify(jobExecutor, after(10000).never()).execute(
            eq(jobId),
            eq(jobGroup),
            eq(jobName),
            eq(testPayload)
        );
    }

    public int getScheduledJobCount() {

        try {

            return quartzScheduler
                .getJobKeys(GroupMatcher.anyGroup())
                .size();

        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    private class TestPayload {

        public String getFoo() {
            return "bar";
        }
    }

}
