package uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

public class JobClassMapperTest {

    @Test
    public void getsCorrectJobMappingByPayloadClass() {
        JobClassMapping jobMapping1 = mock(JobClassMapping.class);
        when(jobMapping1.canHandle(String.class)).thenReturn(false);
        JobClassMapping jobMapping2 = mock(JobClassMapping.class);
        when(jobMapping2.canHandle(String.class)).thenReturn(true);

        JobClassMapper jobMapper = new JobClassMapper(asList(jobMapping1, jobMapping2));
        JobClassMapping jobMapping = jobMapper.getJobMapping(String.class);

        assertThat(jobMapping, is(jobMapping2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotFindMappingForPayloadClass() {
        JobClassMapping jobMapping1 = mock(JobClassMapping.class);
        when(jobMapping1.canHandle(String.class)).thenReturn(false);

        JobClassMapper jobMapper = new JobClassMapper(singletonList(jobMapping1));
        jobMapper.getJobMapping(String.class);
    }
}
