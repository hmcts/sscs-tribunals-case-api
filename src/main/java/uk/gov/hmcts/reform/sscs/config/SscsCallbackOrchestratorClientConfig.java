package uk.gov.hmcts.reform.sscs.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Retryer;
import feign.codec.Decoder;
import feign.jackson.JacksonDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

public class SscsCallbackOrchestratorClientConfig {

    @Bean
    @Primary
    Decoder feignDecoder(ObjectMapper objectMapper) {
        return new JacksonDecoder(objectMapper);
    }

    //decide if needed----
//    @Bean
//    public Retryer retryer(@Value("${feign.client.retryer.period}") long period,
//                           @Value("${feign.client.retryer.maxPeriod}") long maxPeriod,
//                           @Value("${feign.client.retryer.maxAttempts}") int maxAttempts) {
//        return new Retryer.Default(period, maxPeriod, maxAttempts);
//    }

}
