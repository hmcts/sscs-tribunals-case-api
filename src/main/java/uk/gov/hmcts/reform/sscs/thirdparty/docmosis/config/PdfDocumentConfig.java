package uk.gov.hmcts.reform.sscs.thirdparty.docmosis.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "document.pdf")
@Getter
@Setter
public class PdfDocumentConfig {
    private String hmctsImgKey;
    private String hmctsImgVal;
    private String hmctsWelshImgKey;
    private String hmctsWelshImgVal;
}
