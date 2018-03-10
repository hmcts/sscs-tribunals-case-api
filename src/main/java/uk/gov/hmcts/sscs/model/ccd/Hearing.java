package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class Hearing implements Comparable<Hearing> {
    private HearingDetails value;

    @JsonCreator
    public Hearing(@JsonProperty("value") HearingDetails value) {
        this.value = value;
    }

    @Override
    public int compareTo(Hearing o) {
        return value.getHearingDate().compareTo(o.getValue().getHearingDate());
    }
}
