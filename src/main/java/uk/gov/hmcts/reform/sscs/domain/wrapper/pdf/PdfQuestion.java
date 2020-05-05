package uk.gov.hmcts.reform.sscs.domain.wrapper.pdf;

import java.util.Objects;
import uk.gov.hmcts.reform.sscs.domain.wrapper.AnswerState;

public class PdfQuestion {
    private final String questionTitle;
    private final String questionBody;
    private final String answer;
    private final AnswerState answerState;
    private final String issuedDate;
    private final String submittedDate;

    public PdfQuestion(String questionTitle, String questionBody, String answer, AnswerState answerState, String issuedDate, String submittedDate) {
        this.questionTitle = questionTitle;
        this.questionBody = questionBody;
        this.answer = answer;
        this.answerState = answerState;
        this.issuedDate = issuedDate;
        this.submittedDate = submittedDate;
    }

    public String getQuestionTitle() {
        return questionTitle;
    }

    public String getQuestionBody() {
        return questionBody;
    }

    public String getAnswer() {
        return answer;
    }

    public AnswerState getAnswerState() {
        return answerState;
    }

    public String getIssuedDate() {
        return issuedDate;
    }

    public String getSubmittedDate() {
        return submittedDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PdfQuestion that = (PdfQuestion) o;

        if (!Objects.equals(questionTitle, that.questionTitle)) {
            return false;
        }
        if (!Objects.equals(questionBody, that.questionBody)) {
            return false;
        }
        if (!Objects.equals(answer, that.answer)) {
            return false;
        }
        if (answerState != that.answerState) {
            return false;
        }
        if (!Objects.equals(issuedDate, that.issuedDate)) {
            return false;
        }
        return Objects.equals(submittedDate, that.submittedDate);
    }

    @Override
    public int hashCode() {
        int result = questionTitle != null ? questionTitle.hashCode() : 0;
        result = 31 * result + (questionBody != null ? questionBody.hashCode() : 0);
        result = 31 * result + (answer != null ? answer.hashCode() : 0);
        result = 31 * result + (answerState != null ? answerState.hashCode() : 0);
        result = 31 * result + (issuedDate != null ? issuedDate.hashCode() : 0);
        result = 31 * result + (submittedDate != null ? submittedDate.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PdfQuestion{"
                + "questionTitle='" + questionTitle + '\''
                + ", questionBody='" + questionBody + '\''
                + ", answer='" + answer + '\''
                + ", answerState=" + answerState
                + ", issuedDate='" + issuedDate + '\''
                + ", submittedDate='" + submittedDate + '\''
                + '}';
    }
}
