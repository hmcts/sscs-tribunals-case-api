package uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobException;

@RunWith(MockitoJUnitRunner.class)
public class QuartzJobServiceTest {

    private final Scheduler scheduler = mock(Scheduler.class);
    private final QuartzJobService quartzJobService = new QuartzJobService(scheduler);

    @Test
    public void starts_quartz() {

        assertThatCode(
            () -> {

                quartzJobService.start();

                verify(scheduler, times(1)).start();
            }
        ).doesNotThrowAnyException();
    }

    @Test
    public void stops_quartz_with_waiting() {

        assertThatCode(
            () -> {

                quartzJobService.stop(true);

                verify(scheduler, times(1)).shutdown(true);
            }
        ).doesNotThrowAnyException();
    }

    @Test
    public void stops_quartz_without_waiting() {

        assertThatCode(
            () -> {

                quartzJobService.stop(false);

                verify(scheduler, times(1)).shutdown(false);
            }
        ).doesNotThrowAnyException();
    }

    @Test
    public void start_throws_when_quartz_fails() {

        assertThatThrownBy(
            () -> {

                doThrow(SchedulerException.class)
                    .when(scheduler)
                    .start();

                quartzJobService.start();
            }
        ).hasMessage("Cannot start Quartz job scheduler")
            .isExactlyInstanceOf(JobException.class);
    }

    @Test
    public void stop_throws_when_quartz_fails() {

        assertThatThrownBy(
            () -> {

                doThrow(SchedulerException.class)
                    .when(scheduler)
                    .shutdown(true);

                quartzJobService.stop(true);
            }
        ).hasMessage("Cannot stop Quartz job scheduler")
            .isExactlyInstanceOf(JobException.class);
    }

}
