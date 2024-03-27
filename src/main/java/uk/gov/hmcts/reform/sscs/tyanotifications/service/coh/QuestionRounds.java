package uk.gov.hmcts.reform.sscs.tyanotifications.service.coh;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionRounds {
    private int currentQuestionRound;
    private List<QuestionRound> questionRounds;

    public QuestionRounds(@JsonProperty(value = "current_question_round") int currentQuestionRound,
                          @JsonProperty(value = "question_rounds") List<QuestionRound> questionRounds) {
        this.currentQuestionRound = currentQuestionRound;
        this.questionRounds = questionRounds;
    }
}
