package uk.gov.hmcts.reform.sscs.ccd.presubmit;

public enum DwpState {
    WITHDRAWAL_RECEIVED("withdrawalReceived", "Withdrawal received"),
    WITHDRAWN("Withdrawn", "Withdrawn"),
    WITHDRAW_FOR_ACTION("withdrawForAction", "Withdraw for action"),
    FE_RECEIVED("feReceived", "FE received"),
    FE_ACTIONED_NR("feActionedNR", "FE Actioned - NR"),
    FE_ACTIONED_NA("feActionedNA", "FE Actioned - NA"),
    DIRECTION_ACTION_REQUIRED("directionActionRequired", "Direction - action req'd"),
    DIRECTION_RESPONDED("directionResponded", "Direction - responded");

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
