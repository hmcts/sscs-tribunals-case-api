package uk.gov.hmcts.reform.sscs.evidenceshare.config;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


@Configuration
@ConfigurationProperties(prefix = "evidence-share")
@Getter
@Setter
public class EvidenceShareConfig {

    private final List<String> submitTypes = new ArrayList<>();

    private long appealReceivedDelayMs = 5000L;

}
