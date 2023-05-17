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

@Builder(toBuilder = true)
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WriteFinalDecisionTemplateBody {

    @JsonProperty("held_at")
    private String heldAt;
    @JsonProperty("held_before")
    private String heldBefore;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonProperty("held_on")
    private LocalDate heldOn;
    @JsonProperty("is_allowed")
    private boolean isAllowed;
    @JsonProperty("is_set_aside")
    private boolean isSetAside;
    @JsonProperty("date_of_decision")
    private String dateOfDecision;
    @JsonProperty("is_indefinite")
    private boolean isIndefinite;
    @JsonProperty("appellant_name")
    private String appellantName;
    @JsonProperty("appointee_name")
    private String appointeeName;
    @JsonProperty("start_date")
    private String startDate;
    @JsonProperty("end_date")
    private String endDate;
    @JsonProperty("daily_living_is_entitled")
    private boolean dailyLivingIsEntited;
    @JsonProperty("daily_living_is_severely_limited")
    private boolean dailyLivingIsSeverelyLimited;
    @JsonProperty("daily_living_award_rate")
    private String dailyLivingAwardRate;
    @JsonProperty("daily_living_number_of_points")
    private Integer dailyLivingNumberOfPoints;
    @JsonProperty("mobility_is_entitled")
    private boolean mobilityIsEntited;
    @JsonProperty("mobility_is_severely_limited")
    private boolean mobilityIsSeverelyLimited;
    @JsonProperty("mobility_award_rate")
    private String mobilityAwardRate;
    @JsonProperty("mobility_number_of_points")
    private Integer mobilityNumberOfPoints;
    @JsonProperty("daily_living_descriptors")
    private List<Descriptor> dailyLivingDescriptors;
    @JsonProperty("mobility_descriptors")
    private List<Descriptor> mobilityDescriptors;
    @JsonProperty("hearing_type")
    private String hearingType;
    @JsonProperty("page_number")
    private String pageNumber;
    @JsonProperty("attended_hearing")
    private boolean attendedHearing;
    @JsonProperty("appointee_attended")
    private boolean appointeeAttended;
    @JsonProperty("appointee_on_case")
    private boolean appointeeOnCase;
    @JsonProperty("other_party_names_attended_hearing")
    private String otherPartyNamesAttendedHearing;
    @JsonProperty("presenting_officer_attended")
    private boolean presentingOfficerAttended;
    @JsonProperty("is_descriptor_flow")
    private boolean isDescriptorFlow;
    @JsonProperty("details_of_decision")
    private String detailsOfDecision;
    @JsonProperty("reasons_for_decision")
    private List<String> reasonsForDecision;
    @JsonProperty("anything_else")
    private String anythingElse;
    @JsonProperty("esa_is_entitled")
    private boolean esaIsEntited;
    @JsonProperty("esa_number_of_points")
    private Integer esaNumberOfPoints;
    @JsonProperty("esa_schedule_2_descriptors")
    private List<Descriptor> esaSchedule2Descriptors;
    @JsonProperty("esa_schedule_3_descriptors")
    private List<Descriptor> esaSchedule3Descriptors;
    @JsonProperty("esa_award_rate")
    private String esaAwardRate;
    @JsonProperty("is_wca_appeal")
    private boolean wcaAppeal;
    @JsonProperty("is_support_group_only")
    private boolean supportGroupOnly;
    @JsonProperty("is_regulation_29_applicable")
    private Boolean regulation29Applicable;
    @JsonProperty("is_regulation_35_applicable")
    private Boolean regulation35Applicable;
    @JsonProperty("dwp_reassess_the_award")
    private String dwpReassessTheAward;
    @JsonProperty("summary_of_outcome_decision")
    private String summaryOfOutcomeDecision;
    @JsonProperty("uc_is_entitled")
    private boolean ucIsEntited;
    @JsonProperty("uc_number_of_points")
    private Integer ucNumberOfPoints;
    @JsonProperty("uc_schedule_2_descriptors")
    private List<Descriptor> ucSchedule6Descriptors;
    @JsonProperty("uc_schedule_3_descriptors")
    private List<Descriptor> ucSchedule7Descriptors;
    @JsonProperty("uc_award_rate")
    private String ucAwardRate;
    @JsonProperty("is_schedule_8_paragraph_4_applicable")
    private Boolean schedule8Paragraph4Applicable;
    @JsonProperty("is_schedule_9_paragraph_4_applicable")
    private Boolean schedule9Paragraph4Applicable;
    @JsonProperty("is_hmrc")
    private boolean isHmrc;
}
