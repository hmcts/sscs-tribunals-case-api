package uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.GroupMatcher;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobException;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobNotFoundException;

@RunWith(MockitoJUnitRunner.class)
public class QuartzJobRemoverTest {

    private final Scheduler scheduler = mock(Scheduler.class);
    private final QuartzJobRemover quartzJobRemover = new QuartzJobRemover(scheduler);

    @Test
    public void job_is_removed_from_scheduler_by_id() {

        assertThatCode(
            () -> {

                String jobId = "job-id";
                String jobGroup = "job-group";

                when(scheduler.deleteJob(JobKey.jobKey(jobId, jobGroup)))
                    .thenReturn(true);

                quartzJobRemover.remove(jobId, jobGroup);

                verify(scheduler, times(1)).deleteJob(
                    eq(JobKey.jobKey(jobId, jobGroup))
                );
            }
        ).doesNotThrowAnyException();
    }

    @Test
    public void job_is_removed_from_scheduler_by_group() {

        assertThatCode(
            () -> {

                String jobId1 = "job-id-1";
                String jobId2 = "job-id-2";
                String jobGroup = "job-group";

                JobKey jobKey1 = new JobKey(jobId1, jobGroup);
                JobKey jobKey2 = new JobKey(jobId2, jobGroup);

                Set<JobKey> jobKeysFoundAsSet =
                    ImmutableSet.of(jobKey1, jobKey2);

                when(scheduler.getJobKeys(GroupMatcher.groupEquals(jobGroup)))
                    .thenReturn(jobKeysFoundAsSet);

                List<JobKey> jobKeysFoundAsList = new ArrayList<>(jobKeysFoundAsSet);

                when(scheduler.deleteJobs(jobKeysFoundAsList))
                    .thenReturn(true);

                quartzJobRemover.removeGroup(jobGroup);

                verify(scheduler, times(1)).deleteJobs(
                    eq(jobKeysFoundAsList)
                );
            }
        ).doesNotThrowAnyException();
    }

    @Test
    public void remove_job_by_id_throws_when_job_not_found_by_id() {

        assertThatThrownBy(
            () -> {

                String jobId = "missing-job-id";
                String jobGroup = "job-group";

                when(scheduler.deleteJob(JobKey.jobKey(jobId, jobGroup)))
                    .thenReturn(false);

                quartzJobRemover.remove(jobId, jobGroup);
            }

        ).hasMessage("ID: missing-job-id, Group: job-group")
            .isExactlyInstanceOf(JobNotFoundException.class);
    }

    @Test
    public void remove_job_by_id_throws_when_group_has_no_jobs() {

        assertThatThrownBy(
            () -> {

                String jobGroup = "empty-job-group";

                Set<JobKey> jobKeysFoundAsSet =
                    Collections.emptySet();

                when(scheduler.getJobKeys(GroupMatcher.groupEquals(jobGroup)))
                    .thenReturn(jobKeysFoundAsSet);

                List<JobKey> jobKeysFoundAsList = new ArrayList<>(jobKeysFoundAsSet);

                quartzJobRemover.removeGroup(jobGroup);

                verify(scheduler, never()).deleteJobs(
                    eq(any())
                );
            }

        ).hasMessage("Group: empty-job-group")
            .isExactlyInstanceOf(JobNotFoundException.class);
    }

    @Test
    public void remove_job_by_id_throws_when_group_has_jobs_that_cannot_be_found() {

        assertThatThrownBy(
            () -> {

                String jobGroup = "empty-job-group";

                Set<JobKey> jobKeysFoundAsSet =
                    Collections.emptySet();

                when(scheduler.getJobKeys(GroupMatcher.groupEquals(jobGroup)))
                    .thenReturn(jobKeysFoundAsSet);

                List<JobKey> jobKeysFoundAsList = new ArrayList<>(jobKeysFoundAsSet);

                quartzJobRemover.removeGroup(jobGroup);

                verify(scheduler, times(1)).deleteJobs(
                    eq(jobKeysFoundAsList)
                );
            }

        ).hasMessage("Group: empty-job-group")
            .isExactlyInstanceOf(JobNotFoundException.class);
    }

    @Test
    public void remove_job_by_group_throws_when_quartz_fails() {

        assertThatThrownBy(
            () -> {

                String jobId1 = "job-id-1";
                String jobId2 = "job-id-2";
                String jobGroup = "failing-job-group";

                JobKey jobKey1 = new JobKey(jobId1, jobGroup);
                JobKey jobKey2 = new JobKey(jobId2, jobGroup);

                Set<JobKey> jobKeysFoundAsSet =
                    ImmutableSet.of(jobKey1, jobKey2);

                when(scheduler.getJobKeys(GroupMatcher.groupEquals(jobGroup)))
                    .thenReturn(jobKeysFoundAsSet);

                List<JobKey> jobKeysFoundAsList = new ArrayList<>(jobKeysFoundAsSet);

                doThrow(SchedulerException.class)
                    .when(scheduler)
                    .deleteJobs(jobKeysFoundAsList);

                quartzJobRemover.removeGroup(jobGroup);
            }
        ).hasMessage("Error while removing Job by Group. Group: failing-job-group")
            .isExactlyInstanceOf(JobException.class);
    }

    @Test
    public void remove_job_by_id_throws_when_quartz_fails() {

        assertThatThrownBy(
            () -> {

                String jobId = "failing-job-id";
                String jobGroup = "job-group";

                doThrow(SchedulerException.class)
                    .when(scheduler)
                    .deleteJob(JobKey.jobKey(jobId, jobGroup));

                quartzJobRemover.remove(jobId, jobGroup);
            }
        ).hasMessage("Error while removing Job. ID: failing-job-id, Group: job-group")
            .isExactlyInstanceOf(JobException.class);
    }

}
