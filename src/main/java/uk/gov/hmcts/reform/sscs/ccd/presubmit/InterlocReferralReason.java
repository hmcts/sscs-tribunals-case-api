package uk.gov.hmcts.reform.sscs.ccd.presubmit;

public enum InterlocReferralReason {
    TIME_EXTENSION("timeExtension"),
    PHME_REQUEST("phmeRequest"),
    REVIEW_AUDIO_VIDEO_EVIDENCE("reviewAudioVideoEvidence"),
    NONE("none");

    private String id;

    public String getId() {
        return id;
    }

    InterlocReferralReason(String id) {
        this.id = id;
    }
}
