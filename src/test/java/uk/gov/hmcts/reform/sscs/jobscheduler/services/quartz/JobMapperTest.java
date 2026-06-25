package uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class JobMapperTest {
    @Test
    void getCorrectJobMappingByPayload() {
        String payload = "some payload";

        JobMapping<?> jobMapping1 = mock(JobMapping.class);
        when(jobMapping1.canHandle(payload)).thenReturn(false);
        JobMapping<?> jobMapping2 = mock(JobMapping.class);
        when(jobMapping2.canHandle(payload)).thenReturn(true);

        JobMapper jobMapper = new JobMapper(asList(jobMapping1, jobMapping2));
        JobMapping<?> jobMapping = jobMapper.getJobMapping(payload);

        assertThat(jobMapping).isSameAs(jobMapping2);
    }

    @Test
    void cannotFindMappingForPayload() {
        String payload = "some payload";

        JobMapping<?> jobMapping1 = mock(JobMapping.class);
        when(jobMapping1.canHandle(payload)).thenReturn(false);

        JobMapper jobMapper = new JobMapper(singletonList(jobMapping1));

        assertThatIllegalArgumentException().isThrownBy(() -> jobMapper.getJobMapping(payload));
    }
}
