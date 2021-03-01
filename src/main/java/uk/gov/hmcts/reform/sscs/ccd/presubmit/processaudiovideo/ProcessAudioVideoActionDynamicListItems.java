package uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo;

import lombok.Getter;

@Getter
public enum ProcessAudioVideoActionDynamicListItems {

    ISSUE_DIRECTIONS_NOTICE("issueDirectionsNotice", "Issue directions notice"),
    EXCLUDE_EVIDENCE("excludeEvidence", "Exclude audio/video evidence");

    String code;
    String label;

    ProcessAudioVideoActionDynamicListItems(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
