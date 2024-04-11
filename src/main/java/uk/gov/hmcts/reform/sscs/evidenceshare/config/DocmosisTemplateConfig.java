package uk.gov.hmcts.reform.sscs.evidenceshare.config;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;

@Configuration
@ConfigurationProperties(prefix = "docmosis")
@Getter
@Setter
public class DocmosisTemplateConfig {
    Map<LanguagePreference, Map<String, Map<String, String>>> template;
}
