package uk.gov.hmcts.reform.sscs.featureflag;

public enum FeatureFlag {
    SSCS_CHILD_MAINTENANCE_FT("sscs-child-maintenance-ft");

    private final String key;

    FeatureFlag(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
