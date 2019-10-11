package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence;

import lombok.Getter;

@Getter
public enum FurtherEvidenceActionDynamicListItems {
    ISSUE_FURTHER_EVIDENCE("issueFurtherEvidence", "Issue further evidence to all parties"),
    OTHER_DOCUMENT_MANUAL("otherDocumentManual", "Other document type - action manually"),
    INFORMATION_RECEIVED_FOR_INTERLOC_TCW("informationReceivedForInterlocTcw", "Information received for Interloc - send to TCW"),
    INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE("informationReceivedForInterlocJudge", "Information received for Interloc - send to Judge");

    String code;
    String label;

    FurtherEvidenceActionDynamicListItems(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
