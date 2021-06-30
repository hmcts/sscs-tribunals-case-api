package uk.gov.hmcts.reform.sscs.model.tya;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class HearingRecordingRequest {
    private String hearingId;
    private String venue;
    private String hearingDate;
    private String hearingTime;
    private List<HearingRecording> hearingRecordings;
}
