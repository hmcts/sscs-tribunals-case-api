package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

/**
 * Enum specifying the possible types of Award, along with the key for that award in CCD.
 */
public enum AwardType {
    STANDARD_RATE("standardRate"), ENHANCED_RATE("enhancedRate"), NO_AWARD("noAward"), NOT_CONSIDERED("notConsidered"), LOWER_RATE("lowerRate"), HIGHER_RATE("higherRate");

    String key;

    AwardType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
