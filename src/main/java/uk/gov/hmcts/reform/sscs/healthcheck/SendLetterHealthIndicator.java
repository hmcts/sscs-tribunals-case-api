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

@Component("sendLetter")
public class SendLetterHealthIndicator implements HealthIndicator {

    private static final Logger LOG = LoggerFactory.getLogger(SendLetterHealthIndicator.class);

    private final String sendLetterUrl;
    private final RestTemplate restTemplate;

    public SendLetterHealthIndicator(
        @Value("${send-letter.url}") String sendLetterUrl,
        @Qualifier("healthCheckRestTemplate") RestTemplate restTemplate
    ) {
        this.sendLetterUrl = sendLetterUrl;
        this.restTemplate = restTemplate;
    }

    @Override
    public Health health() {
        try {
            restTemplate.getForEntity(sendLetterUrl + "/health", String.class);
            return Health.up().withDetail("url", sendLetterUrl).build();
        } catch (RestClientException e) {
            LOG.error("Health check failed on Send Letter Service", e);
            return Health.down(e).withDetail("url", sendLetterUrl).build();
        }
    }
}
