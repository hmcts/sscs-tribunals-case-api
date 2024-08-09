package uk.gov.hmcts.reform.sscs.model.single.hearing;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HearingDaySchedule {
    private LocalDateTime hearingStartDateTime;

    private LocalDateTime hearingEndDateTime;

    private String listAssistSessionID;

    @JsonProperty("hearingVenueId")
    private String hearingVenueEpimsId;

    private String hearingRoomId;

    private String hearingJudgeId;

    private List<String> panelMemberIds;

    private List<Attendees> attendees;
}
