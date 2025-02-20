package uk.gov.hmcts.reform.sscs.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.sscs.helper.mapping.HearingsDetailsMapping;

@Configuration
public class HearingsDetailsMappingConfig {

    @Value("${feature.direction-hearings.enabled}")
    private boolean isDirectionHearingsEnabled;

    @PostConstruct
    public void init() {
        HearingsDetailsMapping.setDirectionHearingsEnabled(isDirectionHearingsEnabled);
    }
}