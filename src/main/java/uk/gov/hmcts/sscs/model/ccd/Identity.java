package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class Identity {
    private String dob;
    private String nino;

    @JsonCreator
    public Identity(@JsonProperty("dob") String dob,
                    @JsonProperty("nino") String nino) {
        this.dob = dob;
        this.nino = nino;
    }
}
