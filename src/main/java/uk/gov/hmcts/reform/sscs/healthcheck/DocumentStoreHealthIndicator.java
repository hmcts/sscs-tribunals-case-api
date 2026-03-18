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

@Component("documentStore")
public class DocumentStoreHealthIndicator implements HealthIndicator {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentStoreHealthIndicator.class);

    private final String documentManagementUrl;
    private final RestTemplate restTemplate;

    public DocumentStoreHealthIndicator(
        @Value("${document_management.url}") String documentManagementUrl,
        @Qualifier("healthCheckRestTemplate") RestTemplate restTemplate
    ) {
        this.documentManagementUrl = documentManagementUrl;
        this.restTemplate = restTemplate;
    }

    @Override
    public Health health() {
        try {
            restTemplate.getForEntity(documentManagementUrl + "/health", String.class);
            return Health.up().withDetail("url", documentManagementUrl).build();
        } catch (RestClientException e) {
            LOG.error("Health check failed on Document Store", e);
            return Health.down(e).withDetail("url", documentManagementUrl).build();
        }
    }
}
