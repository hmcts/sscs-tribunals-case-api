package uk.gov.hmcts.reform.sscs.model.docassembly;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import java.time.LocalDate;
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
    private String appellantFullName;
    @JsonProperty("appointee_full_name")
    private String appointeeFullName;
    private String nino;
    @JsonProperty("case_id")
    private String caseId;
    @JsonProperty("notice_type")
    private String noticeType;
    @JsonProperty("should_hide_nino")
    private boolean shouldHideNino;
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
    @JsonProperty("date_issued")
    private LocalDate dateIssued;
    @JsonProperty("hmcts2")
    @Builder.Default private String image = ENGLISH_IMAGE;
    @JsonProperty("welshhmcts2")
    @Builder.Default private String welshImage = WELSH_IMAGE;
    @JsonProperty("write_final_decision")
    private WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody;
    @JsonProperty("welsh_date_added")
    private String welshDateAdded;
    @JsonProperty("welsh_generated_date")
    private String welshGeneratedDate;
    @JsonProperty("adjourn_case")
    private AdjournCaseTemplateBody adjournCaseTemplateBody;
    @JsonProperty("write_final_decision_template_content")
    private WriteFinalDecisionTemplateContent writeFinalDecisionTemplateContent;
}
