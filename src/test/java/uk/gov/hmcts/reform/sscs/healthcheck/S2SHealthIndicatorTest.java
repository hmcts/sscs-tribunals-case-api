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
class S2SHealthIndicatorTest {

    private static final String S2S_URL = "http://rpe-service-auth-provider:4502";

    @Mock
    private RestTemplate restTemplate;

    private S2SHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new S2SHealthIndicator(S2S_URL, restTemplate);
    }

    @Test
    void shouldReturnUpWhenS2SIsHealthy() {
        when(restTemplate.getForEntity(S2S_URL + "/health", String.class))
            .thenReturn(ResponseEntity.ok("UP"));

        Health health = healthIndicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(S2S_URL, health.getDetails().get("url"));
    }

    @Test
    void shouldReturnDownWhenS2SIsUnhealthy() {
        when(restTemplate.getForEntity(S2S_URL + "/health", String.class))
            .thenThrow(new RestClientException("Connection refused"));

        Health health = healthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals(S2S_URL, health.getDetails().get("url"));
    }
}
