package uk.gov.hmcts.reform.sscs.tyanotifications.service.coh;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionRound {
    private List<QuestionReferences> questionReferences;

    public QuestionRound(@JsonProperty(value = "question_references") List<QuestionReferences> questionReferences) {
        this.questionReferences = questionReferences;
    }
}
