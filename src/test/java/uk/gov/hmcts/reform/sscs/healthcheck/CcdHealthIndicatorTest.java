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
class CcdHealthIndicatorTest {

    private static final String CCD_API_URL = "http://ccd-data-store-api:4452";

    @Mock
    private RestTemplate restTemplate;

    private CcdHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new CcdHealthIndicator(CCD_API_URL, restTemplate);
    }

    @Test
    void shouldReturnUpWhenCcdIsHealthy() {
        when(restTemplate.getForEntity(CCD_API_URL + "/health", String.class))
            .thenReturn(ResponseEntity.ok("UP"));

        Health health = healthIndicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(CCD_API_URL, health.getDetails().get("url"));
    }

    @Test
    void shouldReturnDownWhenCcdIsUnhealthy() {
        when(restTemplate.getForEntity(CCD_API_URL + "/health", String.class))
            .thenThrow(new RestClientException("Connection refused"));

        Health health = healthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals(CCD_API_URL, health.getDetails().get("url"));
    }
}
