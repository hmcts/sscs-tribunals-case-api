package uk.gov.hmcts.reform.sscs.model.tya;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HearingRecordingResponse {
    private List<CitizenHearingRecording> releasedHearingRecordings;
    private List<CitizenHearingRecording> outstandingHearingRecordings;
    private List<CitizenHearingRecording> requestableHearingRecordings;
}
