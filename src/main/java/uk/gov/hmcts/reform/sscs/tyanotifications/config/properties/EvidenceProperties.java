package uk.gov.hmcts.reform.sscs.tyanotifications.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;


@Configuration
@ConfigurationProperties(prefix = "tya.evidence")
@Getter
public class EvidenceProperties {
    @Getter
    @Setter
    private EvidenceAddress address;

    @Getter
    @Setter
    public static class EvidenceAddress {

        private String line1;
        private String line2;
        private String line3;
        private String scottishLine3;
        private String town;
        private String county;
        private String postcode;
        private String scottishPostcode;
        private String telephone;
        private String telephoneWelsh;
        private String telephoneIbc;
        private boolean scottishPoBoxFeatureEnabled;
        private String ibcAddressLine1;
        private String ibcAddressLine2;
        private String ibcAddressLine3;
        private String ibcAddressPostcode;

        public String getLine2(SscsCaseData ccdResponse) {
            return ccdResponse.isIbcCase() ? getIbcAddressLine2() : getLine2();
        }

        public String getLine3(SscsCaseData ccdResponse) {
            if ("Yes".equalsIgnoreCase(ccdResponse.getIsScottishCase()) && scottishPoBoxFeatureEnabled) {
                return getScottishLine3();
            } else {
                return ccdResponse.isIbcCase() ? getIbcAddressLine3() : getLine3();
            }
        }

        public String getPostcode(SscsCaseData ccdResponse) {
            if ("Yes".equalsIgnoreCase(ccdResponse.getIsScottishCase()) && scottishPoBoxFeatureEnabled) {
                return getScottishPostcode();
            } else {
                return ccdResponse.isIbcCase() ? getIbcAddressPostcode() : getPostcode();
            }
        }
    }
}
