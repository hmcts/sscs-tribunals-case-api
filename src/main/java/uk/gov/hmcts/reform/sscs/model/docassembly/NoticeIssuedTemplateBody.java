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
public class NoticeIssuedTemplateBody implements FormPayload {
    @JsonIgnore
    public static final String SCOTTISH_IMAGE = "[userImage:schmcts.png]";
    @JsonIgnore
    public static final String ENGLISH_IMAGE = "[userImage:enhmcts.png]";
    @JsonIgnore
    public static final String WELSH_IMAGE = "[userImage:welshhmcts.png]";
    @JsonProperty("appellant_full_name")
    String appellantFullName;
    @JsonProperty("appointee_full_name")
    String appointeeFullName;
    String nino;
    @JsonProperty("case_id")
    String caseId;
    @JsonProperty("notice_type")
    String noticeType;
    @JsonProperty("should_hide_nino")
    boolean shouldHideNino;
    @JsonProperty("respondents")
    List<?> respondents;
    @JsonProperty("notice_body")
    String noticeBody;
    @JsonProperty("user_name")
    String userName;
    @JsonProperty("user_role")
    String userRole;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonProperty("date_added")
    LocalDate dateAdded;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonProperty("generated_date")
    LocalDate generatedDate;
    @JsonProperty("date_issued")
    LocalDate dateIssued;
    @JsonProperty("hmcts2")
    @Builder.Default String image = ENGLISH_IMAGE;
    @JsonProperty("welshhmcts2")
    @Builder.Default String welshImage = WELSH_IMAGE;
    @JsonProperty("write_final_decision")
    WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody;
    @JsonProperty("welsh_date_added")
    String welshDateAdded;
    @JsonProperty("welsh_generated_date")
    String welshGeneratedDate;
    @JsonProperty("adjourn_case")
    AdjournCaseTemplateBody adjournCaseTemplateBody;
    @JsonProperty("write_final_decision_template_content")
    WriteFinalDecisionTemplateContent writeFinalDecisionTemplateContent;
    @JsonProperty("idam_surname")
    String idamSurname;
    @JsonProperty("corrected_judge_name")
    String correctedJudgeName;
    @JsonProperty("corrected_generated_date")
    LocalDate correctedGeneratedDate;
    @JsonProperty("corrected_date_issued")
    LocalDate correctedDateIssued;
    @JsonProperty("held_at")
    String heldAt;
    @JsonProperty("held_before")
    String heldBefore;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonProperty("held_on")
    LocalDate heldOn;
}
