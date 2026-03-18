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

@Component("s2s")
public class S2SHealthIndicator implements HealthIndicator {

    private static final Logger LOG = LoggerFactory.getLogger(S2SHealthIndicator.class);

    private final String s2sUrl;
    private final RestTemplate restTemplate;

    public S2SHealthIndicator(
        @Value("${auth.provider.service.client.baseUrl}") String s2sUrl,
        @Qualifier("healthCheckRestTemplate") RestTemplate restTemplate
    ) {
        this.s2sUrl = s2sUrl;
        this.restTemplate = restTemplate;
    }

    @Override
    public Health health() {
        try {
            restTemplate.getForEntity(s2sUrl + "/health", String.class);
            return Health.up().withDetail("url", s2sUrl).build();
        } catch (RestClientException e) {
            LOG.error("Health check failed on S2S Auth", e);
            return Health.down(e).withDetail("url", s2sUrl).build();
        }
    }
}
