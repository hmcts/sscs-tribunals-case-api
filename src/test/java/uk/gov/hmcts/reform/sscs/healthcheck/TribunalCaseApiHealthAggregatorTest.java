package uk.gov.hmcts.reform.sscs.healthcheck;

import static org.mockito.MockitoAnnotations.initMocks;

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
        initMocks(this);
        tribunalCaseApiHealthAggregator = new TribunalCaseApiHealthAggregator();
    }


    @Test
    public void shouldReturnOverallHealthUpWhenHardCheckIsUpAndSoftCheckIsDown() {
        //Given hard dependency
        Health mockCcdDataHealth = Health.up().build();
        Health mockServiceAuth = Health.up().build();

        // soft dependency
        Health mockPdfServiceHealth = Health.down().build();

        Map<String, Health> healths = new HashMap<>();
        healths.put("coreCaseData", mockCcdDataHealth);
        healths.put("serviceAuth", mockServiceAuth);
        healths.put("mockPdfServiceHealth", mockPdfServiceHealth);
        // when
        Health actual = tribunalCaseApiHealthAggregator.aggregate(healths);

        // then
        Assert.assertEquals(Health.up().build().getStatus(), actual.getStatus());
        Assert.assertEquals("{coreCaseData=UP {}, serviceAuth=UP {}, mockPdfServiceHealth=DOWN {}}", actual.getDetails().values().iterator().next().toString());
    }

    @Test
    public void shouldReturnOverallHealthUpWhenHardCheckIsUpAndSoftCheckIsUp() {
        //Given hard dependency
        Health mockCcdDataHealth = Health.up().build();
        Health mockServiceAuth = Health.up().build();

        // soft dependency
        Health mockPdfServiceHealth = Health.up().build();
        Map<String, Health> healths = new HashMap<>();
        healths.put("coreCaseData", mockCcdDataHealth);
        healths.put("serviceAuth", mockServiceAuth);
        healths.put("mockPdfServiceHealth", mockPdfServiceHealth);
        // when
        Health actual = tribunalCaseApiHealthAggregator.aggregate(healths);

        // then
        Assert.assertEquals(Health.up().build().getStatus(), actual.getStatus());
        Assert.assertEquals("{coreCaseData=UP {}, serviceAuth=UP {}, mockPdfServiceHealth=UP {}}", actual.getDetails().values().iterator().next().toString());
    }

    @Test
    public void shouldReturnOverallHealthDownWhenHardCheckIsDown() {
        //Given
        Health mockCcdDataHealth = Health.down(new SocketTimeoutException()).build();
        Health mockServiceAuth = Health.up().build();

        Health mockPdfServiceHealth = Health.up().build();
        Map<String, Health> healths = new HashMap<>();
        healths.put("coreCaseData", mockCcdDataHealth);
        healths.put("serviceAuth", mockServiceAuth);
        healths.put("mockPdfServiceHealth", mockPdfServiceHealth);
        // when
        Health actual = tribunalCaseApiHealthAggregator.aggregate(healths);

        // then
        Assert.assertEquals(Health.down().build().getStatus(), actual.getStatus());
        Assert.assertEquals("{coreCaseData=DOWN {error=java.net.SocketTimeoutException: null}, serviceAuth=UP {}, mockPdfServiceHealth=UP {}}",
                actual.getDetails().values().iterator().next().toString());
    }

}
