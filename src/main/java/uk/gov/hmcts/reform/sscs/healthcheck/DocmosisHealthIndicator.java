package uk.gov.hmcts.reform.sscs.healthcheck;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class DocmosisHealthIndicator implements HealthIndicator {

    private static final Logger LOG = LoggerFactory.getLogger(DocmosisHealthIndicator.class);

    private final String docmosisStatusUri;
    private final RestTemplate restTemplate;

    public DocmosisHealthIndicator(
        @Value("${service.pdf-service.health.uri}") String docmosisStatusUri,
        RestTemplate restTemplate
    ) {
        this.docmosisStatusUri = docmosisStatusUri;
        this.restTemplate = restTemplate;
    }

    public Health health() {

        try {

            Map<String, Object> response =
                restTemplate
                    .exchange(
                        docmosisStatusUri,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<Map<String, Object>>() {
                        }
                    ).getBody();

            if (response != null && response.containsKey("ready")) {
                Object readyValue = response.get("ready");
                if ((readyValue instanceof String && "true".equalsIgnoreCase((String) readyValue))
                    || (readyValue instanceof Boolean && Boolean.TRUE.equals(readyValue))) {
                    return new Health.Builder().up().build();
                } else {
                    return new Health.Builder().down().build();
                }
            } else {
                return new Health.Builder().down().build();
            }
        } catch (RestClientException e) {

            LOG.error("Error performing Docmosis healthcheck", e);
            return new Health.Builder().down(e).build();
        }
    }
}
