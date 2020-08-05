package uk.gov.hmcts.reform.sscs.service.coversheet;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

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
    @JsonProperty("hmcts2")
    private final String hmcts;
    @JsonProperty("welshhmcts2")
    private final String welshhmcts;

    public PdfCoverSheet(String caseId,
                         String name,
                         String addressLine1,
                         String addressLine2,
                         String addressTown,
                         String addressCounty,
                         String addressPostcode,
                         String hmcts,
                         String welshhmcts) {
        this.caseId = caseId;
        this.name = name;
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.addressTown = addressTown;
        this.addressCounty = addressCounty;
        this.addressPostcode = addressPostcode;
        this.hmcts = hmcts;
        this.welshhmcts =  welshhmcts;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PdfCoverSheet that = (PdfCoverSheet) o;
        return Objects.equals(caseId, that.caseId)
                && Objects.equals(name, that.name)
                && Objects.equals(addressLine1, that.addressLine1)
                && Objects.equals(addressLine2, that.addressLine2)
                && Objects.equals(addressTown, that.addressTown)
                && Objects.equals(addressCounty, that.addressCounty)
                && Objects.equals(addressPostcode, that.addressPostcode)
                && Objects.equals(hmcts, that.hmcts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(caseId, name, addressLine1, addressLine2, addressTown, addressCounty, addressPostcode, hmcts);
    }
}
