package uk.gov.hmcts.reform.sscs.bulkscan.handler;

import lombok.Getter;

@Getter
public enum InterlocReferralReasonOptions {
    OVER_13_MONTHS("over13months"), OVER_13_MONTHS_AND_GROUNDS_MISSING("over13MonthsAndGroundsMissing");
    private final String value;

    InterlocReferralReasonOptions(String value) {
        this.value = value;
    }
}
