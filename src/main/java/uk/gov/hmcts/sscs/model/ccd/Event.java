package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class Event {
    String date;
    String type;
    String description;

    @JsonCreator
    public Event(@JsonProperty("date") String date,
                 @JsonProperty("type") String type,
                 @JsonProperty("description") String description) {
        this.date = date;
        this.type = type;
        this.description = description;
    }
}
