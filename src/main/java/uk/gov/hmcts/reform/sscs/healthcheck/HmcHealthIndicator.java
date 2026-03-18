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

@Component("hmc")
public class HmcHealthIndicator implements HealthIndicator {

    private static final Logger LOG = LoggerFactory.getLogger(HmcHealthIndicator.class);

    private final String hmcUrl;
    private final RestTemplate restTemplate;

    public HmcHealthIndicator(
        @Value("${hmc.url}") String hmcUrl,
        @Qualifier("healthCheckRestTemplate") RestTemplate restTemplate
    ) {
        this.hmcUrl = hmcUrl;
        this.restTemplate = restTemplate;
    }

    @Override
    public Health health() {
        try {
            restTemplate.getForEntity(hmcUrl + "/health", String.class);
            return Health.up().withDetail("url", hmcUrl).build();
        } catch (RestClientException e) {
            LOG.error("Health check failed on HMC Hearings", e);
            return Health.down(e).withDetail("url", hmcUrl).build();
        }
    }
}
