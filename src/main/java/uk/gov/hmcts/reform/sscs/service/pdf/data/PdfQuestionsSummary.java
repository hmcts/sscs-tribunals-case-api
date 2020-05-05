package uk.gov.hmcts.reform.sscs.service.pdf.data;

import java.util.List;
import java.util.Objects;
import uk.gov.hmcts.reform.sscs.domain.wrapper.QuestionSummary;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfAppealDetails;

public class PdfQuestionsSummary {
    private final PdfAppealDetails pdfAppealDetails;
    private final List<QuestionSummary> questions;

    public PdfQuestionsSummary(PdfAppealDetails pdfAppealDetails, List<QuestionSummary> questions) {
        this.pdfAppealDetails = pdfAppealDetails;
        this.questions = questions;
    }

    public PdfAppealDetails getAppealDetails() {
        return pdfAppealDetails;
    }

    public List<QuestionSummary> getQuestions() {
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
        PdfQuestionsSummary that = (PdfQuestionsSummary) o;
        return Objects.equals(pdfAppealDetails, that.pdfAppealDetails)
                && Objects.equals(questions, that.questions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pdfAppealDetails, questions);
    }
}
