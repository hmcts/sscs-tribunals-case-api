package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

public enum HearingType {

    FACE_TO_FACE("faceToFace", "face to face");

    final String key;
    final String value;

    HearingType(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public static HearingType getByKey(String key) {
        for (HearingType mapping : HearingType.values()) {
            if (mapping.key.equals(key)) {
                return mapping;
            }
        }
        throw new IllegalArgumentException("Unknown HearingType for key:" + key);
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

}
