package uk.gov.hmcts.reform.sscs;

import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty("feature.log-selected-properties.enabled")
public class EnvironmentPropertiesPrinter {

    @Autowired
    private Environment env;

    @PostConstruct
    public void logApplicationProperties() {
        log.info("{}={}", "robotics.email.to", env.getProperty("robotics.email.to"));
        log.info("{}={}", "robotics.email.from", env.getProperty("robotics.email.from"));
        log.info("{}={}", "robotics.email.pipAeTo", env.getProperty("robotics.email.pipAeTo"));
        log.info("{}={}", "robotics.email.scottishTo", env.getProperty("robotics.email.scottishTo"));
        log.info("{}={}", "ld.sdk-key", env.getProperty("ld.sdk-key"));
        log.info("{}={}", "send-grid.apiKey", env.getProperty("send-grid.apiKey"));
    }
}
