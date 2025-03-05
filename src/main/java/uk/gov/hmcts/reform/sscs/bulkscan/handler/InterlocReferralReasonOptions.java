package uk.gov.hmcts.reform.sscs.bulkscan.handler;

public enum InterlocReferralReasonOptions {
    OVER_13_MONTHS("over13months"), OVER_13_MONTHS_AND_GROUNDS_MISSING("over13MonthsAndGroundsMissing");
    private String value;

    public String getValue() {
        return value;
    }

    InterlocReferralReasonOptions(String value) {
        this.value = value;
    }
}
