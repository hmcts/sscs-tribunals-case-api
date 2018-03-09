package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class Events implements Comparable<Events> {
    Event value;

    @JsonCreator
    public Events(@JsonProperty("value") Event value) {
        this.value = value;
    }

    @Override
    public int compareTo(Events o) {
        return value.getDate().compareTo(o.getValue().getDate());
    }
}
