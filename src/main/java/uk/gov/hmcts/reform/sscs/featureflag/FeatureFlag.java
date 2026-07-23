package uk.gov.hmcts.reform.sscs.featureflag;

public enum FeatureFlag {
    SSCS_CHILD_MAINTENANCE_FT("sscs-cm-feature");

    private final String key;

    FeatureFlag(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
