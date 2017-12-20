package uk.gov.hmcts.sscs.domain.corecase;

public enum TribunalType {

    ORAL, PAPER;

    @Override
    public String toString() {
        return name().substring(0, 1).toUpperCase() + name().substring(1).toLowerCase();
    }

    public static TribunalType getTribunalByKey(String x) {
        TribunalType t = null;
        for (TribunalType type : TribunalType.values()) {
            if (type.toString().equals(x)) {
                t = type;
            }
        }
        return t;
    }
}
