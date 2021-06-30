package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadhearingrecording;

public enum HearingTypeForRecording {

    ADJOURNED("adjourned", "Adjourned"),
    FINAL("final", "Final");

    private final String key;
    private final String value;

    HearingTypeForRecording(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}