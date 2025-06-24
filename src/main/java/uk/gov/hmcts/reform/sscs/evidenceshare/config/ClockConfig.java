package uk.gov.hmcts.reform.sscs.evidenceshare.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone(); // or Clock.systemUTC()
    }
}
