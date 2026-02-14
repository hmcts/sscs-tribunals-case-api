package uk.gov.hmcts.reform.sscs.notifications.bulkprint.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "exela")
@Getter
@Setter
public class ExelaAddressConfig {
    private String addressLine1;
    private String addressLine2;
    private String addressLine3;
    private String addressPostcode;
    private String scottishAddressLine2;
    private String scottishPostcode;
    private String ibcAddressLine1;
    private String ibcAddressLine2;
    private String ibcAddressPostcode;
}
