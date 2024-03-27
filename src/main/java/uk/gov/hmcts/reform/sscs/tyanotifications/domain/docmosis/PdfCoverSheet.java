package uk.gov.hmcts.reform.sscs.tyanotifications.domain.docmosis;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class PdfCoverSheet {
    @JsonProperty("case_id")
    private final String caseId;
    @JsonProperty("name")
    private String name;
    @JsonProperty("address_line1")
    private final String addressLine1;
    @JsonProperty("address_line2")
    private final String addressLine2;
    @JsonProperty("address_town")
    private final String addressTown;
    @JsonProperty("address_county")
    private final String addressCounty;
    @JsonProperty("address_postcode")
    private final String addressPostcode;
    @JsonProperty("excela_address_line1")
    private final String excelaAddressLine1;
    @JsonProperty("excela_address_line2")
    private final String excelaAddressLine2;
    @JsonProperty("excela_address_line3")
    private final String excelaAddressLine3;
    @JsonProperty("excela_address_postcode")
    private final String excelaAddressPostcode;
    @JsonProperty("hmcts2")
    private final String hmcts;
    @JsonProperty("hmctsWelshImgVal")
    private final String hmctsWelshImgVal;

    public PdfCoverSheet(String caseId,
                         String name,
                         String addressLine1,
                         String addressLine2,
                         String addressTown,
                         String addressCounty,
                         String addressPostcode,
                         String excelaAddressLine1,
                         String excelaAddressLine2,
                         String excelaAddressLine3,
                         String excelaAddressPostcode,
                         String hmcts,
                         String hmctsWelshImgVal) {
        this.caseId = caseId;
        this.name = name;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.addressTown = addressTown;
        this.addressCounty = addressCounty;
        this.addressPostcode = addressPostcode;
        this.excelaAddressLine1 = excelaAddressLine1;
        this.excelaAddressLine2 = excelaAddressLine2;
        this.excelaAddressLine3 = excelaAddressLine3;
        this.excelaAddressPostcode = excelaAddressPostcode;
        this.hmcts = hmcts;
        this.hmctsWelshImgVal = hmctsWelshImgVal;
    }
}
