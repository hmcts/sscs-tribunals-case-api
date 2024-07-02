package uk.gov.hmcts.reform.sscs.tyanotifications.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "letter-async")
@Getter
@Setter
public class LetterAsyncConfigProperties {
    private int maxAttempts;
    private long delay;
    private double multiplier;
    private long maxDelay;
    private long initialDelay;
}