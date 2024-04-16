package uk.gov.hmcts.reform.sscs.evidenceshare.config;

import com.launchdarkly.sdk.server.LDClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LaunchDarklyConfiguration {

    @Bean
    public LDClient ldClient(@Value("${ld.sdk-key}") String sdkKey) {
        return new LDClient(sdkKey);
    }
}
