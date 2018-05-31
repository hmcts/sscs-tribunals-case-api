package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class Appellant {

    private Name name;
    private Address address;
    private Contact contact;
    private Identity identity;
    private String isAppointee;

    @JsonCreator
    public Appellant(@JsonProperty("name") Name name,
                     @JsonProperty("address") Address address,
                     @JsonProperty("contact") Contact contact,
                     @JsonProperty("identity") Identity identity,
                     @JsonProperty("isAppointee") String isAppointee) {
        this.name = name;
        this.address = address;
        this.contact = contact;
        this.identity = identity;
        this.isAppointee = isAppointee;
    }
}
