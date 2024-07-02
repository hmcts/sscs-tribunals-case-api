package uk.gov.hmcts.reform.sscs.tyanotifications.config;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("retry")
@Getter
@Setter
public class TyanRetryConfig {
    private Integer max;
    private Map<Integer, Integer> delayInSeconds;
}
