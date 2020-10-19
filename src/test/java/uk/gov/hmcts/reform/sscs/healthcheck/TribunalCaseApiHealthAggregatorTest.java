package uk.gov.hmcts.reform.sscs.healthcheck;

import static org.mockito.MockitoAnnotations.openMocks;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.actuate.health.Health;

public class TribunalCaseApiHealthAggregatorTest {

    private TribunalCaseApiHealthAggregator tribunalCaseApiHealthAggregator;

    @Before
    public void setUp() {
        openMocks(this);
        tribunalCaseApiHealthAggregator = new TribunalCaseApiHealthAggregator();
    }


    @Test
    public void shouldReturnOverallHealthUpWithDetailMessageUp() {
        //Given
        Health mockCcdDataHealth = Health.up().build();
        Health mockPdfServiceHealth = Health.up().build();
        Map<String, Health> healths = new HashMap<>();
        healths.put("mockCcdDataHealth", mockCcdDataHealth);
        healths.put("mockPdfServiceHealth", mockPdfServiceHealth);
        // when
        Health actual = tribunalCaseApiHealthAggregator.aggregate(healths);

        // then
        Assert.assertEquals(Health.up().build().getStatus(), actual.getStatus());
        Assert.assertEquals("UP {}", actual.getDetails().values().iterator().next().toString());
    }

    @Test
    public void shouldReturnOverallHealthUpWithDetailMessageWithException() {
        //Given
        Health mockCcdDataHealth = Health.down(new SocketTimeoutException()).build();
        Health mockPdfServiceHealth = Health.up().build();
        Map<String, Health> healths = new HashMap<>();
        healths.put("mockCcdDataHealth", mockCcdDataHealth);
        healths.put("mockPdfServiceHealth", mockPdfServiceHealth);
        // when
        Health actual = tribunalCaseApiHealthAggregator.aggregate(healths);

        // then
        Assert.assertEquals(Health.up().build().getStatus(), actual.getStatus());
        Assert.assertEquals("{mockCcdDataHealth=DOWN {error=java.net.SocketTimeoutException: null}, mockPdfServiceHealth=UP {}}",
                actual.getDetails().values().iterator().next().toString());
    }

}
