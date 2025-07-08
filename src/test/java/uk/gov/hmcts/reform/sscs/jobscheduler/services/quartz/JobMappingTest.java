package uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Predicate;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobExecutor;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobPayloadDeserializer;

public class JobMappingTest {

    private JobPayloadDeserializer<String> jobPayloadDeserializer;
    private JobExecutor<String> jobExecutor;
    private Predicate<String> jobCanBeMapped;
    private Predicate<String> jobCannotBeMapped;
    private String payloadSource;

    @Before
    public void setUp() {
        jobPayloadDeserializer = mock(JobPayloadDeserializer.class);
        jobExecutor = mock(JobExecutor.class);
        jobCanBeMapped = x -> true;
        jobCannotBeMapped = x -> false;
        payloadSource = "payloadSource";
    }

    @Test
    public void mappingCanHandlePayloadByPayload() {
        JobMapping<String> jobMapping = new JobMapping<>(jobCanBeMapped, jobPayloadDeserializer, jobExecutor);

        boolean canHandle = jobMapping.canHandle(payloadSource);

        assertThat(canHandle, is(true));
    }

    @Test
    public void mappingCannotHandlePayloadByPayload() {
        JobMapping<String> jobMapping = new JobMapping<>(jobCannotBeMapped, jobPayloadDeserializer, jobExecutor);

        boolean canHandle = jobMapping.canHandle(payloadSource);

        assertThat(canHandle, is(false));
    }

    @Test
    public void deserializesAndExecutesJob() {
        JobMapping<String> jobMapping = new JobMapping<>(jobCanBeMapped, jobPayloadDeserializer, jobExecutor);

        String deserializedPayload = "deserialized payload";
        when(jobPayloadDeserializer.deserialize(payloadSource)).thenReturn(deserializedPayload);

        String jobId = "jobId";
        String jobGroup = "jobGroup";
        String jobName = "jobName";
        jobMapping.execute(jobId, jobGroup, jobName, payloadSource);

        verify(jobExecutor).execute(jobId, jobGroup, jobName, deserializedPayload);
    }
}
