package uk.gov.hmcts.reform.sscs.jobscheduler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.time.ZonedDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.idam.client.IdamApi;
import uk.gov.hmcts.reform.idam.client.OidcApi;
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

@ExtendWith(SpringExtension.class)
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
    private OidcApi oidcApi;

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

    @BeforeEach
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

        assertEquals(0, getScheduledJobCount(), "Job scheduler is empty at start");

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

        assertEquals(1, getScheduledJobCount(), "Job was scheduled into Quartz");

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

        assertEquals(0, getScheduledJobCount(), "Job scheduler is empty at start");

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

        assertEquals(2, getScheduledJobCount(), "Jobs were scheduled into Quartz");

        jobRemover.removeGroup(jobGroup);

        assertEquals(0, getScheduledJobCount(), "Jobs were removed from Quartz after execution");

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

        assertEquals(0, getScheduledJobCount(), "Job scheduler is empty at start");

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

        assertEquals(1, getScheduledJobCount(), "Job was scheduled into Quartz");

        jobRemover.remove(jobId, jobGroup);

        assertEquals(0, getScheduledJobCount(), "Job was removed from Quartz after execution");

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