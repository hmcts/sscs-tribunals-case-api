package uk.gov.hmcts.reform.sscs.ccd.presubmit;

public enum InterlocReferralReason {
    TIME_EXTENSION("timeExtension");
    private String id;

    public String getId() {
        return id;
    }

    InterlocReferralReason(String id) {
        this.id = id;
    }
}