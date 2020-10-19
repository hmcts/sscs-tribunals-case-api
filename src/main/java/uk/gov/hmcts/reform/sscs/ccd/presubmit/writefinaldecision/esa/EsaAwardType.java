package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

/**
 * Enum specifying the possible types of Award, along with the key for that award in CCD.
 */
public enum EsaAwardType {
    LOWER_RATE("lowerRate"), HIGHER_RATE("higherRate"), NO_AWARD("noAward");

    String key;

    EsaAwardType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
