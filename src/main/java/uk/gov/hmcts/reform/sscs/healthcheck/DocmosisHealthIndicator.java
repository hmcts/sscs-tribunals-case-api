package uk.gov.hmcts.reform.sscs.healthcheck;

import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class DocmosisHealthIndicator implements HealthIndicator {


    private final String docmosisStatusUri;
    private final RestTemplate restTemplate;

    public DocmosisHealthIndicator(
            @Value("${docmosis.health.endpoint}") String docmosisStatusUri,
            RestTemplate restTemplate) {
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

            if (response != null
                    && response.containsKey("ready")
                    && "true".equalsIgnoreCase((String) response.get("ready"))) {

                return new Health.Builder().up().build();
            } else {
                return new Health.Builder().down().build();
            }

        } catch (RestClientException e) {
            log.error("Error performing Docmosis healthcheck", e);
            return new Health.Builder().down(e).build();
        }
    }
}
