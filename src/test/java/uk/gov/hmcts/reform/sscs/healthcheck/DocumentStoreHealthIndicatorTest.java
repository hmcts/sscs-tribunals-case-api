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
class DocumentStoreHealthIndicatorTest {

    private static final String DM_STORE_URL = "http://dm-store:5005";

    @Mock
    private RestTemplate restTemplate;

    private DocumentStoreHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new DocumentStoreHealthIndicator(DM_STORE_URL, restTemplate);
    }

    @Test
    void shouldReturnUpWhenDocumentStoreIsHealthy() {
        when(restTemplate.getForEntity(DM_STORE_URL + "/health", String.class))
            .thenReturn(ResponseEntity.ok("UP"));

        Health health = healthIndicator.health();

        assertEquals(Status.UP, health.getStatus());
        assertEquals(DM_STORE_URL, health.getDetails().get("url"));
    }

    @Test
    void shouldReturnDownWhenDocumentStoreIsUnhealthy() {
        when(restTemplate.getForEntity(DM_STORE_URL + "/health", String.class))
            .thenThrow(new RestClientException("Connection refused"));

        Health health = healthIndicator.health();

        assertEquals(Status.DOWN, health.getStatus());
        assertEquals(DM_STORE_URL, health.getDetails().get("url"));
    }
}
