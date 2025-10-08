package uk.gov.hmcts.reform.sscs.domain.wrapper;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.AUTO;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AssociateCaseDetails {
    @Schema(example = "subscription email address", requiredMode = REQUIRED)
    @JsonProperty(value = "email")
    private String email;
    @Schema(example = "appellant postcode", requiredMode = AUTO)
    @JsonProperty(value = "postcode")
    private String postcode;
    @Schema(example = "IBCA reference", requiredMode = AUTO)
    @JsonProperty(value = "ibcaReference")
    private String ibcaReference;

    //Needed for Jackson
    private AssociateCaseDetails() {

    }

    public AssociateCaseDetails(String email, String postcode, String ibcaReference) {
        this.email = email;
        this.postcode = postcode;
        this.ibcaReference = ibcaReference;
    }

    public String getEmail() {
        return email;
    }

    public String getPostcode() {
        return postcode;
    }

    public String getIbcaReference() {
        return ibcaReference;
    }
}
