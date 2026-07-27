package uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobExecutor;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobPayloadDeserializer;

@ExtendWith(MockitoExtension.class)
class JobMappingTest {

    @Mock
    private JobPayloadDeserializer<String> jobPayloadDeserializer;
    @Mock
    private JobExecutor<String> jobExecutor;
    private Predicate<String> jobCanBeMapped;
    private Predicate<String> jobCannotBeMapped;
    private String payloadSource;

    @BeforeEach
    void setUp() {
        jobCanBeMapped = x -> true;
        jobCannotBeMapped = x -> false;
        payloadSource = "payloadSource";
    }

    @Test
    void mappingCanHandlePayloadByPayload() {
        JobMapping<String> jobMapping = new JobMapping<>(jobCanBeMapped, jobPayloadDeserializer, jobExecutor);

        boolean canHandle = jobMapping.canHandle(payloadSource);
        
        assertThat(canHandle).isTrue();
    }

    @Test
    void mappingCannotHandlePayloadByPayload() {
        JobMapping<String> jobMapping = new JobMapping<>(jobCannotBeMapped, jobPayloadDeserializer, jobExecutor);

        boolean canHandle = jobMapping.canHandle(payloadSource);

        assertThat(canHandle).isFalse();
    }

    @Test
    void deserializesAndExecutesJob() {
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
