package uk.gov.hmcts.reform.sscs.config;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;

@Component
@Configuration
@ConfigurationProperties
@Setter
@Getter
public class DocumentConfiguration {

    private Map<LanguagePreference, Map<EventType,String>> documents;
    private Map<String, Map<LanguagePreference, Map<EventType,String>>> benefitSpecificDocuments;
    private Map<LanguagePreference, Map<String, String>> evidence;
    private Map<LanguagePreference, String> cover;
}
