package uk.gov.hmcts.reform.sscs.model.docassembly;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

@Builder(toBuilder = true)
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdjournCaseTemplateBody {

    @JsonProperty("held_at")
    private String heldAt;
    @JsonProperty("held_before")
    private String heldBefore;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonProperty("held_on")
    private LocalDate heldOn;

    @JsonProperty("appellant_name")
    private String appellantName;

    @JsonProperty("hearing_type")
    private String hearingType;

    @JsonProperty("next_hearing_venue")
    private String nextHearingVenue;

    @JsonProperty("next_hearing_at_venue")
    private boolean nextHearingAtVenue;

    @JsonProperty("next_hearing_type")
    private String nextHearingType;

    @JsonProperty("next_hearing_date")
    private String nextHearingDate;

    @JsonProperty("next_hearing_timeslot")
    private String nextHearingTimeslot;

    @JsonProperty("panel_members_excluded")
    private String panelMembersExcluded;

    @JsonProperty("tribunal_direct_po_to_attend")
    private YesNo tribunalDirectPoToAttend;

    @JsonProperty("reasons_for_decision")
    private List<String> reasonsForDecision;

    @JsonProperty("additional_directions")
    private List<String> additionalDirections;

    @JsonProperty("interpreter_description")
    private String interpreterDescription;

}
