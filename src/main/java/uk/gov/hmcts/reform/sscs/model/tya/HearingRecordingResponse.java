package uk.gov.hmcts.reform.sscs.model.tya;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HearingRecordingResponse {
    private List<HearingRecordingRequest> releasedHearingRecordings;
    private List<HearingRecordingRequest> outstandingHearingRecordings;
    private List<HearingRecordingRequest> requestableHearingRecordings;
}
