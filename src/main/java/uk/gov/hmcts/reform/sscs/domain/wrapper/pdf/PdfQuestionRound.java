package uk.gov.hmcts.reform.sscs.domain.wrapper.pdf;

import java.util.List;
import java.util.Objects;

public class PdfQuestionRound {
    private final List<PdfQuestion> questions;

    public PdfQuestionRound(List<PdfQuestion> questions) {
        this.questions = questions;
    }

    public List<PdfQuestion> getQuestions() {
        return questions;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PdfQuestionRound that = (PdfQuestionRound) o;
        return Objects.equals(questions, that.questions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(questions);
    }

    @Override
    public String toString() {
        return "PdfQuestionRound{"
                + "questions=" + questions
                + '}';
    }
}
