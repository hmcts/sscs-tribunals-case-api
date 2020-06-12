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
    @JsonProperty("reasons_for_decision")
    private String reasonsForDecision;
}
