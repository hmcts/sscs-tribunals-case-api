package uk.gov.hmcts.reform.sscs.ccd.presubmit.postpone;

public enum DwpState {
    HEARING_POSTPONED("hearingPostponed");

    private String value;

    public String getValue() {
        return value;
    }


    DwpState(String value) {
        this.value = value;
    }
}
