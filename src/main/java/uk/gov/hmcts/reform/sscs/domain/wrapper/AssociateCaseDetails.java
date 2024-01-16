package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AssociateCaseDetails {
    @Schema(example = "subscription email address", required = true)
    @JsonProperty(value = "email")
    private String email;
    @Schema(example = "appellant postcode", required = true)
    @JsonProperty(value = "postcode")
    private String postcode;

    //Needed for Jackson
    private AssociateCaseDetails() {

    }

    public AssociateCaseDetails(String email, String postcode) {
        this.email = email;
        this.postcode = postcode;
    }

    public String getEmail() {
        return email;
    }

    public String getPostcode() {
        return postcode;
    }
}
