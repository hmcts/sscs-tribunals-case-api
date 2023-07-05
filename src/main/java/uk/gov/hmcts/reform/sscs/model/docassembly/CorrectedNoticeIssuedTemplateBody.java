package uk.gov.hmcts.reform.sscs.model.docassembly;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CorrectedNoticeIssuedTemplateBody extends NoticeIssuedTemplateBody {
    @JsonProperty("corrected_judge_name")
    private String correctedJudgeName;
    @JsonProperty("corrected_generated_date")
    private LocalDate correctedGeneratedDate;
    @JsonProperty("corrected_date_issued")
    private LocalDate correctedDateIssued;
}
