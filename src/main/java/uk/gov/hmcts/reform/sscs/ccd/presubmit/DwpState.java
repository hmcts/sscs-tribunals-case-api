package uk.gov.hmcts.reform.sscs.ccd.presubmit;

public enum DwpState {
    FE_RECEIVED("feReceived"), FE_ACTIONED_NR("feActionedNR");
    private String value;

    public String getValue() {
        return value;
    }

    DwpState(String value) {
        this.value = value;
    }
}