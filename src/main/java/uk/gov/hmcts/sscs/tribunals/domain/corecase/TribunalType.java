package uk.gov.hmcts.sscs.tribunals.domain.corecase;

public enum TribunalType {

    ORAL("Oral"), PAPER("Paper");

    private final String key;

    TribunalType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static TribunalType getTribunalByKey(String x) {
        TribunalType t = null;
        for (TribunalType type : TribunalType.values()) {
            if (type.getKey().equals(x)) {
                t = type;
            }
        }
        return t;
    }
}
