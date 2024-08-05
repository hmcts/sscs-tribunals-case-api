package uk.gov.hmcts.reform.sscs.tyanotifications.config;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.domain.LanguagePreference;


@Component
@ConfigurationProperties("pdf-service")
@Getter
@Setter
public class DocmosisTemplatesConfig {

    private Map<LanguagePreference, Map<String, String>> coversheets;
    private String hmctsImgVal;
    private String hmctsImgKey;
    private String hmctsWelshImgVal;
    private String hmctsWelshImgKey;
}
