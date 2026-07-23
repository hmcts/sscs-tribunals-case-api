package uk.gov.hmcts.reform.sscs.jobscheduler.services.quartz;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;

public class JobMapperTest {
    @Test
    public void getCorrectJobMappingByPayload() {
        String payload = "some payload";

        JobMapping jobMapping1 = mock(JobMapping.class);
        when(jobMapping1.canHandle(payload)).thenReturn(false);
        JobMapping jobMapping2 = mock(JobMapping.class);
        when(jobMapping2.canHandle(payload)).thenReturn(true);

        JobMapper jobMapper = new JobMapper(asList(jobMapping1, jobMapping2));
        JobMapping jobMapping = jobMapper.getJobMapping(payload);

        assertThat(jobMapping, is(jobMapping2));
    }

    @Test(expected = IllegalArgumentException.class)
    public void cannotFindMappingForPayload() {
        String payload = "some payload";

        JobMapping jobMapping1 = mock(JobMapping.class);
        when(jobMapping1.canHandle(payload)).thenReturn(false);

        JobMapper jobMapper = new JobMapper(singletonList(jobMapping1));
        jobMapper.getJobMapping(payload);
    }
}
