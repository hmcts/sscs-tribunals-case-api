package uk.gov.hmcts.sscs.model.idam;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

@Value
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDetails {
    String id;

    public UserDetails(@JsonProperty("id") String id) {
        this.id = id;
    }
}
