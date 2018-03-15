package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Address {
    private String line1;
    private String line2;
    private String town;
    private String county;
    private String postcode;

    @JsonCreator
    public Address(@JsonProperty("line1") String line1,
                   @JsonProperty("line2") String line2,
                   @JsonProperty("town") String town,
                   @JsonProperty("county") String county,
                   @JsonProperty("postcode") String postcode) {
        this.line1 = line1;
        this.line2 = line2;
        this.town = town;
        this.county = county;
        this.postcode = postcode;
    }
}
