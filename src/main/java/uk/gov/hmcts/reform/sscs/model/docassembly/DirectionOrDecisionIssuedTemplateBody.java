package uk.gov.hmcts.reform.sscs.model.docassembly;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.reform.docassembly.domain.FormPayload;

@Builder(toBuilder = true)
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DirectionOrDecisionIssuedTemplateBody implements FormPayload {
    @JsonIgnore
    public static final String SCOTTISH_IMAGE = "[userImage:schmcts2.png]";
    @JsonIgnore
    public static final String ENGLISH_IMAGE = "[userImage:enhmcts.png]";
    @JsonProperty("appellant_full_name")
    private String appellantFullName;
    private String nino;
    @JsonProperty("case_id")
    private String caseId;
    @JsonProperty("notice_type")
    private String noticeType;
    @JsonProperty("notice_body")
    private String noticeBody;
    @JsonProperty("user_name")
    private String userName;
    @JsonProperty("user_role")
    private String userRole;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonProperty("date_added")
    private LocalDate dateAdded;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonProperty("generated_date")
    private LocalDate generatedDate;
    @JsonProperty("hmcts2")
    @Builder.Default private String image = ENGLISH_IMAGE;
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
}
