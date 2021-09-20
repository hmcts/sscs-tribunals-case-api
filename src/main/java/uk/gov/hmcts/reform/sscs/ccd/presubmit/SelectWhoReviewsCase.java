package uk.gov.hmcts.reform.sscs.ccd.presubmit;

public enum SelectWhoReviewsCase {
    REVIEW_BY_TCW("reviewByTcw", "Review by TCW"),
    REVIEW_BY_JUDGE("reviewByJudge", "Review by Judge"),
    POSTPONEMENT_REQUEST_INTERLOC_SEND_TO_TCW("postponementRequestInterlocSendToTcw", "Postponement request for interloc - send to TCW");

    private String id;
    private String label;

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    SelectWhoReviewsCase(String id, String label) {
        this.id = id;
        this.label = label;
    }
}