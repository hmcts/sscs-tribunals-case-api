package uk.gov.hmcts.reform.sscs.ccd.presubmit;

public enum DwpState {
    FE_RECEIVED("feReceived", "FE received"),
    FE_ACTIONED_NR("feActionedNR", "FE Actioned - NR"),
    FE_ACTIONED_NA("feActionedNA", "FE Actioned - NA");
    private String value;
    private String label;

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    DwpState(String value, String label) {
        this.value = value;
        this.label = label;
    }
}