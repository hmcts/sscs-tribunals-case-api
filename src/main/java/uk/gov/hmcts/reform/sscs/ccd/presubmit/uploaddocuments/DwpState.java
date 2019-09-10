package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

public enum DwpState {
    FE_RECEIVED("feReceived");
    private String value;

    public String getValue() {
        return value;
    }

    DwpState(String value) {
        this.value = value;
    }
}