package uk.gov.hmcts.reform.sscs.ccd.presubmit.dwpuploadresponse;

public enum ElementsDisputed {

    GENERAL("general", "General"),
    STANDARD_ALLOWANCE_SANCTIONS("standardAllowanceSanctions", "Standard allowance - sanctions"),
    STANDARD_ALLOWANCE_OVERPAYMENT("standardAllowanceOverpayment", "Standard allowance - overpayment"),
    HOUSING("housing", "Housing"),
    CHILDCARE("childcare", "Childcare"),
    CARE("carerElement", "Carer"),
    CHILD_ELEMENT("childElement", "Child"),
    CHILD_DISABLED("disabledChildren", "Disabled child addition"),
    LIMITED_CAPABILITY_FOR_WORK("limitedCapabilityWork", "Limited Capability for Work (WCA)");

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
