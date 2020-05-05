package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;

@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QuestionRound {
    private List<QuestionSummary> questions;
    private final String deadlineExpiryDate;
    private int deadlineExtensionCount = 0;

    public QuestionRound(List<QuestionSummary> questions, String deadlineExpiryDate,
                         int deadlineExtensionCount) {
        this.questions = questions;
        this.deadlineExpiryDate = deadlineExpiryDate;
        this.deadlineExtensionCount = deadlineExtensionCount;
    }

    @JsonProperty(value = "questions")
    public List<QuestionSummary> getQuestions() {
        return questions;
    }

    @JsonProperty(value = "deadline_expiry_date")
    public String getDeadlineExpiryDate() {
        return deadlineExpiryDate;
    }

    @JsonProperty(value = "deadline_extension_count")
    public int getDeadlineExtensionCount() {
        return deadlineExtensionCount;
    }

    public static QuestionRound emptyQuestionRound() {
        return new QuestionRound(Collections.emptyList(), null, 0);
    }
}
