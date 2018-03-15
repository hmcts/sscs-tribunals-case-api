package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class Venue {
    private String name;
    private Address address;
    private String googleMapLink;

    @JsonCreator
    public Venue(@JsonProperty("name") String name,
                   @JsonProperty("address") Address address,
                   @JsonProperty("googleMapLink") String googleMapLink) {
        this.name = name;
        this.address = address;
        this.googleMapLink = googleMapLink;
    }
}
