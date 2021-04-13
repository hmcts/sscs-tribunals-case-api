package uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo;

import lombok.Getter;

@Getter
public enum ProcessAudioVideoActionDynamicListItems {

    ISSUE_DIRECTIONS_NOTICE("issueDirectionsNotice", "Issue directions notice"),
    ADMIT_EVIDENCE("admitEvidence", "Admit audio/video evidence"),
    EXCLUDE_EVIDENCE("excludeEvidence", "Exclude audio/video evidence"),
    SEND_TO_JUDGE("sendToJudge", "Send to judge"),
    SEND_TO_ADMIN("sendToAdmin", "Send to admin");

    String code;
    String label;

    ProcessAudioVideoActionDynamicListItems(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
