package uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobPayloadSerializer;

class JobClassMappingTest {

    private JobPayloadSerializer<String> jobPayloadSerializer;

    @BeforeEach
    void setUp() {
        jobPayloadSerializer = mock(JobPayloadSerializer.class);
    }

    @Test
    void mappingCanHandlePayloadByClass() {
        JobClassMapping<String> jobMapping = new JobClassMapping<>(String.class, jobPayloadSerializer);

        boolean canHandle = jobMapping.canHandle(String.class);

        assertThat(canHandle).isTrue();
    }

    @Test
    void mappingCannotHandlePayloadByClass() {
        JobClassMapping<String> jobMapping = new JobClassMapping<>(String.class, jobPayloadSerializer);

        boolean canHandle = jobMapping.canHandle(Integer.class);

        assertThat(canHandle).isFalse();
    }

    @Test
    void serialize() {
        JobClassMapping<String> jobMapping = new JobClassMapping<>(String.class, jobPayloadSerializer);

        String payload = "payload";
        jobMapping.serialize(payload);
        verify(jobPayloadSerializer).serialize(payload);
    }
}
