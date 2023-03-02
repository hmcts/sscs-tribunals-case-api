package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import lombok.Getter;

@Getter
public enum FurtherEvidenceActionDynamicListItems {
    ISSUE_FURTHER_EVIDENCE("issueFurtherEvidence", "Issue further evidence to all parties"),
    OTHER_DOCUMENT_MANUAL("otherDocumentManual", "Other document type - action manually"),
    INFORMATION_RECEIVED_FOR_INTERLOC_TCW("informationReceivedForInterlocTcw",
        "Information received for Interloc - send to TCW"),
    INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE("informationReceivedForInterlocJudge",
        "Information received for Interloc - send to Judge"),
    SEND_TO_INTERLOC_REVIEW_BY_JUDGE("sendToInterlocReviewByJudge", "Send to Interloc - Review by Judge"),
    SEND_TO_INTERLOC_REVIEW_BY_TCW("sendToInterlocReviewByTcw", "Send to Interloc - Review by Tcw"),
    ADMIN_ACTION_CORRECTION("adminActionCorrection", "Admin action correction");

    String code;
    String label;

    FurtherEvidenceActionDynamicListItems(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
