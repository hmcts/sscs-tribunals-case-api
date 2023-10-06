package uk.gov.hmcts.reform.sscs.ccd.presubmit.adjourncase;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum HearingType {

    FACE_TO_FACE("faceToFace", "face to face hearing", true),
    TELEPHONE("telephone", "telephone hearing", true),
    VIDEO("video", "video hearing", true),
    PAPER("paper", "decision on the papers", false);

    final String key;
    final String value;
    final boolean isOralHearingType;

    public static HearingType getByKey(String key) {
        for (HearingType mapping : HearingType.values()) {
            if (mapping.key.equals(key)) {
                return mapping;
            }
        }
        throw new IllegalArgumentException("Unknown HearingType for key:" + key);
    }
    
    public String getValue() {
        return value;
    }

    public String getKey() {
        return key;
    }

    public boolean isOralHearingType() {
        return isOralHearingType;
    }

}
