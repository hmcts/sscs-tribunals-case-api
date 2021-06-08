package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadhearingrecording;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@JsonIgnoreProperties(ignoreUnknown = true)
@Value
@Builder(toBuilder = true)
public class HearingRecordingRequest {
    String requestingParty;
    String requestedHearing;

    @JsonCreator
    public HearingRecordingRequest(@JsonProperty("requestingParty") String requestingParty,
                                   @JsonProperty("requestedHearing") String requestedHearing) {
        this.requestingParty = requestingParty;
        this.requestedHearing = requestedHearing;
    }
}
