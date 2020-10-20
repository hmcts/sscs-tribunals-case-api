package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

/**
 * Enum specifying the possible types of Award, along with the key for that award in CCD.
 */
public enum PipAwardType {
    STANDARD_RATE("standardRate"), ENHANCED_RATE("enhancedRate"), NO_AWARD("noAward"), NOT_CONSIDERED("notConsidered");

    String key;

    PipAwardType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
