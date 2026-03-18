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

@Component("idam")
public class IdamHealthIndicator implements HealthIndicator {

    private static final Logger LOG = LoggerFactory.getLogger(IdamHealthIndicator.class);

    private final String idamApiUrl;
    private final RestTemplate restTemplate;

    public IdamHealthIndicator(
        @Value("${idam.api.url}") String idamApiUrl,
        @Qualifier("healthCheckRestTemplate") RestTemplate restTemplate
    ) {
        this.idamApiUrl = idamApiUrl;
        this.restTemplate = restTemplate;
    }

    @Override
    public Health health() {
        try {
            restTemplate.getForEntity(idamApiUrl + "/health", String.class);
            return Health.up().withDetail("url", idamApiUrl).build();
        } catch (RestClientException e) {
            LOG.error("Health check failed on IDAM API", e);
            return Health.down(e).withDetail("url", idamApiUrl).build();
        }
    }
}
