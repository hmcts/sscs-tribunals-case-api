package uk.gov.hmcts.reform.sscs.healthcheck;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component("ccd")
public class CcdHealthIndicator implements HealthIndicator {

    private static final Logger LOG = LoggerFactory.getLogger(CcdHealthIndicator.class);

    private final String ccdApiUrl;
    private final RestTemplate restTemplate;

    public CcdHealthIndicator(
        @Value("${core_case_data.api.url}") String ccdApiUrl,
        @Qualifier("healthCheckRestTemplate") RestTemplate restTemplate
    ) {
        this.ccdApiUrl = ccdApiUrl;
        this.restTemplate = restTemplate;
    }

    @Override
    public Health health() {
        try {
            restTemplate.getForEntity(ccdApiUrl + "/health", String.class);
            return Health.up().withDetail("url", ccdApiUrl).build();
        } catch (RestClientException e) {
            LOG.error("Health check failed on CCD Data Store", e);
            return Health.down(e).withDetail("url", ccdApiUrl).build();
        }
    }
}
