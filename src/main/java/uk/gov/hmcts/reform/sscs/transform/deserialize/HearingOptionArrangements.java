package uk.gov.hmcts.reform.sscs.transform.deserialize;

public enum HearingOptionArrangements {
    SIGN_LANGUAGE_INTERPRETER("signLanguageInterpreter"),
    HEARING_LOOP("hearingLoop"),
    DISABLE_ACCESS("disabledAccess");

    private String value;

    public String getValue() {
        return value;
    }

    HearingOptionArrangements(String value) {
        this.value = value;
    }
}