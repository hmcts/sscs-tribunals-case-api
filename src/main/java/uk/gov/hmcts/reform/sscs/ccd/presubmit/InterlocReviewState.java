package uk.gov.hmcts.reform.sscs.ccd.presubmit;

public enum InterlocReviewState {
    REVIEW_BY_TCW("reviewByTcw"), AWAITING_INFORMATION("awaitingInformation"),
    REVIEW_BY_JUDGE("reviewByJudge"), NONE("none"),
    AWAITING_ADMIN_ACTION("awaitingAdminAction"),
    WELSH_TRANSLATION("welshTranslation");
    private String id;

    public String getId() {
        return id;
    }

    InterlocReviewState(String id) {
        this.id = id;
    }
}