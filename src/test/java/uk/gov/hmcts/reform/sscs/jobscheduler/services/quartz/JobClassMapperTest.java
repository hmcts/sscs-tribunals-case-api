package uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

class JobClassMapperTest {

    @Test
    void getsCorrectJobMappingByPayloadClass() {
        JobClassMapping<?> jobMapping1 = mock(JobClassMapping.class);
        when(jobMapping1.canHandle(String.class)).thenReturn(false);
        JobClassMapping<?> jobMapping2 = mock(JobClassMapping.class);
        when(jobMapping2.canHandle(String.class)).thenReturn(true);

        JobClassMapper jobMapper = new JobClassMapper(asList(jobMapping1, jobMapping2));
        JobClassMapping<?> jobMapping = jobMapper.getJobMapping(String.class);

        assertThat(jobMapping).isSameAs(jobMapping2);
    }

    @Test
    void cannotFindMappingForPayloadClass() {
        JobClassMapping<?> jobMapping1 = mock(JobClassMapping.class);
        when(jobMapping1.canHandle(String.class)).thenReturn(false);

        JobClassMapper jobMapper = new JobClassMapper(singletonList(jobMapping1));

        assertThatIllegalArgumentException()
            .isThrownBy(() -> jobMapper.getJobMapping(String.class));
    }
}
