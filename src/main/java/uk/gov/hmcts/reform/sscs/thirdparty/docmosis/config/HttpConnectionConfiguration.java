package uk.gov.hmcts.reform.sscs.thirdparty.docmosis.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.ResourceHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConditionalOnProperty("http.connect.timeout")
public class HttpConnectionConfiguration {

    @Value("${http.connect.timeout}")
    private int httpConnectTimeout;

    @Value("${http.connect.request.timeout}")
    private int httpConnectRequestTimeout;

    @Value("${http.connect.read.timeout}")
    private int httpConnectReadTimeout;

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());
        restTemplate.getMessageConverters().add(new FormHttpMessageConverter());
        restTemplate.getMessageConverters().add(new ResourceHttpMessageConverter());

        restTemplate.setRequestFactory(getClientHttpRequestFactory());

        return restTemplate;
    }

    private ClientHttpRequestFactory getClientHttpRequestFactory() {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(httpConnectTimeout))
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(httpConnectRequestTimeout))
                .build();

        var client = HttpClientBuilder
                .create()
                .useSystemProperties()
                .setDefaultRequestConfig(config)
                .build();

        return new HttpComponentsClientHttpRequestFactory(client);
    }
}
