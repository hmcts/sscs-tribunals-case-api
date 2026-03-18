package uk.gov.hmcts.reform.sscs.healthcheck;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class HealthCheckConfig {

    private static final int HEALTH_CHECK_TIMEOUT_MS = 3000;

    @Bean("healthCheckRestTemplate")
    public RestTemplate healthCheckRestTemplate() {
        var timeout = Timeout.ofMilliseconds(HEALTH_CHECK_TIMEOUT_MS);
        RequestConfig config = RequestConfig.custom()
            .setConnectTimeout(timeout)
            .setConnectionRequestTimeout(timeout)
            .build();

        HttpClient httpClient = HttpClientBuilder.create()
            .useSystemProperties()
            .setDefaultRequestConfig(config)
            .build();

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
        return restTemplate;
    }
}
