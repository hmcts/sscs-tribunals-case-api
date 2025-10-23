package uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.quartz.JobBuilder.newJob;

import com.google.common.collect.ImmutableMap;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import wiremock.com.google.common.collect.Maps;

@RunWith(MockitoJUnitRunner.class)
public class QuartzFailedJobReschedulerTest {

    private static final int MAX_NUMBER_OF_ATTEMPTS = 5;
    private static final Duration DELAY_BETWEEN_ATTEMPTS = Duration.ofMillis(1000);
    private static final String ATTEMPT_JOB_DATA_KEY = "attempt";

    private final JobExecutionException jobExecutionException = new JobExecutionException("test");

    private final QuartzFailedJobRescheduler rescheduler = new QuartzFailedJobRescheduler(
        MAX_NUMBER_OF_ATTEMPTS,
        DELAY_BETWEEN_ATTEMPTS
    );

    @Test
    public void getName_does_not_throw_exception() {
        assertThatCode(
            () -> rescheduler.getName()
        ).doesNotThrowAnyException();
    }

    @Test
    public void jobToBeExecuted_does_not_throw_exception() {
        assertThatCode(
            () -> rescheduler.jobToBeExecuted(createContext())
        ).doesNotThrowAnyException();
    }

    @Test
    public void jobExecutionVetoed_does_not_throw_exception() {
        assertThatCode(
            () -> rescheduler.jobExecutionVetoed(createContext())
        ).doesNotThrowAnyException();
    }

    @Test
    public void jobWasExecuted_should_not_reschedule_successful_job() throws Exception {
        Scheduler scheduler = mock(Scheduler.class);
        JobExecutionContext context = createContext(
            scheduler,
            createJobDetail(),
            convertToJobDataMap(ImmutableMap.of(ATTEMPT_JOB_DATA_KEY, 1))
        );

        rescheduler.jobWasExecuted(context, null);

        verify(scheduler, never()).scheduleJob(any());
    }

    @Test
    public void jobWasExecuted_should_reschedule_failed_job() throws Exception {
        // given
        int lastAttempt = 2;
        JobDetail jobDetail = createJobDetail();
        Map<String, Object> originalJobDataMap = createSampleMap(lastAttempt);
        JobDataMap jobDataMap = convertToJobDataMap(originalJobDataMap);
        Scheduler scheduler = mock(Scheduler.class);
        JobExecutionContext context = createContext(scheduler, jobDetail, jobDataMap);

        // when
        rescheduler.jobWasExecuted(context, jobExecutionException);

        //then
        Instant rescheduleTime = Instant.now();

        ArgumentCaptor<Trigger> triggerCaptor = ArgumentCaptor.forClass(Trigger.class);
        verify(scheduler).scheduleJob(triggerCaptor.capture());

        Trigger trigger = triggerCaptor.getValue();
        assertTriggerStartTimeIsCorrect(trigger, rescheduleTime, DELAY_BETWEEN_ATTEMPTS, 100);
        assertSameMapWithIncrementedAttempt(trigger.getJobDataMap(), originalJobDataMap);
        assertThat(trigger.getJobKey()).isEqualTo(jobDetail.getKey());
    }

    @Test
    public void jobWasExecuted_should_not_reschedule_failed_job_too_many_times() throws Exception {
        // given
        JobDetail jobDetail = createJobDetail();
        JobDataMap jobDataMap =
            convertToJobDataMap(ImmutableMap.of(ATTEMPT_JOB_DATA_KEY, MAX_NUMBER_OF_ATTEMPTS));

        Scheduler scheduler = mock(Scheduler.class);
        JobExecutionContext context = createContext(scheduler, jobDetail, jobDataMap);

        // when
        rescheduler.jobWasExecuted(context, jobExecutionException);

        // then
        verify(scheduler, never()).scheduleJob(any());
    }

    private void assertTriggerStartTimeIsCorrect(
        Trigger trigger,
        Instant startOfDelayPeriod,
        Duration expectedDelay,
        long accuracyInMs
    ) {
        Duration actualDelay =
            Duration.between(startOfDelayPeriod, trigger.getStartTime().toInstant());

        // check if trigger is set to fire after the expected delay, with 100-millisecond tolerance
        assertThat(expectedDelay.minus(actualDelay)).isLessThan(Duration.ofMillis(accuracyInMs));
    }

    private void assertSameMapWithIncrementedAttempt(
        Map<String, Object> compared,
        Map<String, Object> original
    ) {
        Map<String, Object> expected = incrementAttempt(original);

        assertThat(compared).containsAllEntriesOf(expected);
        assertThat(compared).hasSameSizeAs(expected);
    }

    private Map<String, Object> incrementAttempt(Map<String, Object> map) {
        Map<String, Object> updatedMap = Maps.newHashMap(map);
        updatedMap.put(ATTEMPT_JOB_DATA_KEY, (int) map.get(ATTEMPT_JOB_DATA_KEY) + 1);
        return updatedMap;
    }

    private JobExecutionContext createContext() {
        return createContext(
            mock(Scheduler.class),
            createJobDetail(),
            convertToJobDataMap(ImmutableMap.of("params", "some params"))
        );
    }

    private JobExecutionContext createContext(
        Scheduler scheduler,
        JobDetail jobDetail,
        JobDataMap jobDataMap
    ) {
        JobExecutionContext context = mock(JobExecutionContext.class);

        given(context.getJobDetail()).willReturn(jobDetail);
        given(context.getMergedJobDataMap()).willReturn(jobDataMap);
        given(context.getScheduler()).willReturn(scheduler);

        return context;
    }

    private JobDetail createJobDetail() {
        return newJob(Job.class)
            .withIdentity("id123")
            .build();
    }

    private JobDataMap convertToJobDataMap(Map<?, ?> innerMap) {
        return new JobDataMap(innerMap);
    }

    private Map<String, Object> createSampleMap(int attempt) {
        return ImmutableMap.of(
            "params", "some params",
            ATTEMPT_JOB_DATA_KEY, attempt,
            "more params", "..."
        );
    }
}
