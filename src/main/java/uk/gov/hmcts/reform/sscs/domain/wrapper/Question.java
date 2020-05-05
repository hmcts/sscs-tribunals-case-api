package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Objects;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Question {

    private final String onlineHearingId;
    private final String questionId;
    private final int questionOrdinal;
    private final String questionHeaderText;
    private final String questionBodyText;
    private final String answerId;
    private final String answer;
    private final AnswerState answerState;
    private final String answerDate;
    private final List<Evidence> evidence;

    public Question(String onlineHearingId,
                    String questionId,
                    int questionOrdinal,
                    String questionHeaderText,
                    String questionBodyText,
                    String answerId,
                    String answer,
                    AnswerState answerState,
                    String answerDate,
                    List<Evidence> evidence) {
        this.onlineHearingId = onlineHearingId;
        this.questionId = questionId;
        this.questionOrdinal = questionOrdinal;
        this.questionHeaderText = questionHeaderText;
        this.questionBodyText = questionBodyText;
        this.answerId = answerId;
        this.answer = answer;
        this.answerState = answerState;
        this.answerDate = answerDate;
        this.evidence = evidence;
    }

    @ApiModelProperty(example = "ID_1", required = true)
    @JsonProperty(value = "online_hearing_id")
    public String getOnlineHearingId() {
        return onlineHearingId;
    }

    @ApiModelProperty(example = "ID_1", required = true)
    @JsonProperty(value = "question_id")
    public String getQuestionId() {
        return questionId;
    }

    @ApiModelProperty(example = "1", required = true)
    @JsonProperty(value = "question_ordinal")
    public int getQuestionOrdinal() {
        return questionOrdinal;
    }

    @ApiModelProperty(example = "A question header", required = true)
    @JsonProperty(value = "question_header_text")
    public String getQuestionHeaderText() {
        return questionHeaderText;
    }

    @ApiModelProperty(example = "A question body", required = true)
    @JsonProperty(value = "question_body_text")
    public String getQuestionBodyText() {
        return questionBodyText;
    }

    public String getAnswerId() {
        return answerId;
    }

    @ApiModelProperty(example = "An answer to a question")
    @JsonProperty(value = "answer")
    public String getAnswer() {
        return answer;
    }

    @ApiModelProperty(required = true)
    @JsonProperty(value = "answer_state")
    public AnswerState getAnswerState() {
        return answerState;
    }

    @ApiModelProperty(required = true)
    @JsonProperty(value = "answer_date")
    public String getAnswerDate() {
        return answerDate;
    }

    @ApiModelProperty(required = true)
    @JsonProperty(value = "evidence")
    public List<Evidence> getEvidence() {
        return evidence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Question question = (Question) o;
        return questionOrdinal == question.questionOrdinal
                && Objects.equals(onlineHearingId, question.onlineHearingId)
                && Objects.equals(questionId, question.questionId)
                && Objects.equals(questionHeaderText, question.questionHeaderText)
                && Objects.equals(questionBodyText, question.questionBodyText)
                && Objects.equals(evidence, question.evidence)
                && Objects.equals(answerId, question.answerId)
                && Objects.equals(answer, question.answer)
                && answerState == question.answerState
                && Objects.equals(answerDate, question.answerDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(onlineHearingId, questionId, questionOrdinal, questionHeaderText, questionBodyText, evidence, answerId, answer, answerState, answerDate);
    }

    @Override
    public String toString() {
        return "Question{"
                + "onlineHearingId='" + onlineHearingId + '\''
                + ", questionId='" + questionId + '\''
                + ", questionOrdinal=" + questionOrdinal
                + ", questionHeaderText='" + questionHeaderText + '\''
                + ", questionBodyText='" + questionBodyText + '\''
                + ", evidence=" + evidence
                + ", answerId='" + answerId + '\''
                + ", answer='" + answer + '\''
                + ", answerState=" + answerState
                + ", answerDate='" + answerDate + '\''
                + '}';
    }
}
