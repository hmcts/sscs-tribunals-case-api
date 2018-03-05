package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class Doc {
    private String dateReceived;
    private String description;

    @JsonCreator
    public Doc(@JsonProperty("dateReceived") String dateReceived,
               @JsonProperty("description") String description) {
        this.dateReceived = dateReceived;
        this.description = description;
    }
}
