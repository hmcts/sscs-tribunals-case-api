package uk.gov.hmcts.reform.sscs.domain.wrapper;

public enum ComparedRate {
    Higher("higher"),
    Lower("lower"),
    Same("same");

    String key;

    ComparedRate(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static ComparedRate getByKey(String key) {
        for (ComparedRate comparedRate : ComparedRate.values()) {
            if (comparedRate.key.equals(key)) {
                return comparedRate;
            }
        }
        throw new IllegalArgumentException("Unknown ComparedRate for key:" + key);
    }
}
