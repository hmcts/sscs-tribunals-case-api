package uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo;

import lombok.Getter;

@Getter
public enum ProcessAudioVideoActionDynamicListItems {

    ISSUE_DIRECTIONS_NOTICE("issueDirectionsNotice", "Issue directions notice"),
    EXCLUDE_EVIDENCE("excludeEvidence", "Exclude audio/video evidence"),
    SEND_TO_JUDGE("sendToJudge", "Send to judge");

    String code;
    String label;

    ProcessAudioVideoActionDynamicListItems(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
