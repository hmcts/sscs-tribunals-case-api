package uk.gov.hmcts.reform.sscs.model.docassembly;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Builder(toBuilder = true)
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Descriptor {

    @JsonProperty("activity_question_number")
    private String activityQuestionNumber;
    @JsonProperty("activity_question_value")
    private String activityQuestionValue;
    @JsonProperty("activity_answer_letter")
    private String activityAnswerLetter;
    @JsonProperty("activity_answer_value")
    private String activityAnswerValue;
    @JsonProperty("activity_answer_points")
    private int activityAnswerPoints;
}
