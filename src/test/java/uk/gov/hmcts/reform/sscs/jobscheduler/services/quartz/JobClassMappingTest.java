package uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.jobscheduler.services.JobPayloadSerializer;

public class JobClassMappingTest {

    private JobPayloadSerializer<String> jobPayloadSerializer;

    @Before
    public void setUp() {
        jobPayloadSerializer = mock(JobPayloadSerializer.class);
    }

    @Test
    public void mappingCanHandlePayloadByClass() {
        JobClassMapping<String> jobMapping = new JobClassMapping<>(String.class, jobPayloadSerializer);

        boolean canHandle = jobMapping.canHandle(String.class);

        assertThat(canHandle, is(true));
    }

    @Test
    public void mappingCannotHandlePayloadByClass() {
        JobClassMapping<String> jobMapping = new JobClassMapping<>(String.class, jobPayloadSerializer);

        boolean canHandle = jobMapping.canHandle(Integer.class);

        assertThat(canHandle, is(false));
    }

    @Test
    public void serialize() {
        JobClassMapping<String> jobMapping = new JobClassMapping<>(String.class, jobPayloadSerializer);

        String payload = "payload";
        jobMapping.serialize(payload);
        verify(jobPayloadSerializer).serialize(payload);
    }
}
