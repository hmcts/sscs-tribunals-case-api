package uk.gov.hmcts.reform.sscs.model.tya;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CitizenHearingRecording {
    private String hearingId;
    private String venue;
    private String hearingDate;
    private List<HearingRecording> hearingRecordings;
}
