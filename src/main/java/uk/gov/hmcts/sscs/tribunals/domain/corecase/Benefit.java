package uk.gov.hmcts.sscs.tribunals.domain.corecase;

public enum Benefit {

    UNIVERSAL_CREDIT("Universal Credit", "001"),
    PIP("PIP", "002"),
    INCOME_SUPPORT("Income Support", "077"),
    CARERS_ALLOWANCE("Carers Allowance", "070"),
    JOB_SEEKERS_ALLOWANCE("Job Seekers Allowance", "073");

    private final String type;
    private final String code;

    Benefit(String type, String code) {
        this.type = type;
        this.code = code;
    }

    public String getType() {
        return type;
    }

    public String getCode() {
        return code;
    }

    public static Benefit getBenefitByType(String x) {
        Benefit b = null;
        for (Benefit type : Benefit.values()) {
            if (type.getType().equals(x)) {
                b = type;
            }
        }
        return b;
    }
}
