package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

/**
 * Enum to encapsulate binding between an activity question key, and the corresponding getter method on SscsCaseData that yields that answer to that question.
 */
public enum ElementsDisputed {

    GENERAL("general", "General"),
    STANDARD_ALLOWANCE_SANCTIONS("standardAllowanceSanctions", "Standard allowance - sanctions"),
    STANDARD_ALLOWANCE_OVERPAYMENT("standardAllowanceOverpayment", "Standard allowance - overpayment"),
    HOUSING("housing", "Housing"),
    CHILDCARE("childcare", "Childcare"),
    CARE("carerElement", "Carer"),
    CHILD_ELEMENT("childElement", "Child"),
    CHILD_DISABLED("disabledChildren", "Disabled child addition");

    private final String key;
    private final String value;

    ElementsDisputed(String key, String value) {
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
