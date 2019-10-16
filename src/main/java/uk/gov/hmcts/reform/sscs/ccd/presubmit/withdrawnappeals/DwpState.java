package uk.gov.hmcts.reform.sscs.ccd.presubmit.withdrawnappeals;

public enum DwpState {
    WITHDRAWAL_RECEIVED("withdrawalReceived"),
    WITHDRAWN("Withdrawn"),
    WITHDRAW_FOR_ACTION("withdrawForAction");
    private String value;

    public String getValue() {
        return value;
    }

    DwpState(String value) {
        this.value = value;
    }
}