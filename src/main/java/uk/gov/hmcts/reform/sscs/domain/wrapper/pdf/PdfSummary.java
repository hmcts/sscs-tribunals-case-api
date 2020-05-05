package uk.gov.hmcts.reform.sscs.domain.wrapper.pdf;

import java.util.List;
import java.util.Objects;

public class PdfSummary {
    private final PdfAppealDetails appealDetails;
    private final String relistingReason;
    private final List<PdfQuestionRound> questionRounds;

    public PdfSummary(PdfAppealDetails appealDetails, String relistingReason, List<PdfQuestionRound> questionRounds) {
        this.appealDetails = appealDetails;
        this.relistingReason = relistingReason;
        this.questionRounds = questionRounds;
    }

    public PdfAppealDetails getAppealDetails() {
        return appealDetails;
    }


    public String getRelistingReason() {
        return relistingReason;
    }

    public List<PdfQuestionRound> getQuestionRounds() {
        return questionRounds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PdfSummary that = (PdfSummary) o;
        return Objects.equals(appealDetails, that.appealDetails)
                && Objects.equals(relistingReason, that.relistingReason)
                && Objects.equals(questionRounds, that.questionRounds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appealDetails, relistingReason, questionRounds);
    }

    @Override
    public String toString() {
        return "PdfSummary{"
                + "appealDetails=" + appealDetails
                + ", relistingReason='" + relistingReason + '\''
                + ", questionRounds=" + questionRounds
                + '}';
    }
}
