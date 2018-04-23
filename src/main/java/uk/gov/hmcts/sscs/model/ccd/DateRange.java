package uk.gov.hmcts.sscs.model.ccd;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder
public class DateRange {
    private String start;
    private String end;

    @JsonCreator
    public DateRange(@JsonProperty("start") String start,
                     @JsonProperty("end") String end) {
        this.start = start;
        this.end = end;
    }

}
