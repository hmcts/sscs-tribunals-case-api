package uk.gov.hmcts.reform.sscs.healthcheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class HmcHealthIndicatorTest {

    private static final String HMC_URL = "http://hmc-cft-hearing-service:8084";

    @Mock
    private RestTemplate restTemplate;

    private HmcHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new HmcHealthIndicator(HMC_URL, restTemplate);
    }

    @Test
    void shouldReturnUpWhenHmcIsHealthy() {
        when(restTemplate.getForEntity(HMC_URL + "/health", String.class))
            .thenReturn(ResponseEntity.ok("UP"));

        Health health = healthIndicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(HMC_URL, health.getDetails().get("url"));
    }

    @Test
    void shouldReturnDownWhenHmcIsUnhealthy() {
        when(restTemplate.getForEntity(HMC_URL + "/health", String.class))
            .thenThrow(new RestClientException("Connection refused"));

        Health health = healthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals(HMC_URL, health.getDetails().get("url"));
    }
}
