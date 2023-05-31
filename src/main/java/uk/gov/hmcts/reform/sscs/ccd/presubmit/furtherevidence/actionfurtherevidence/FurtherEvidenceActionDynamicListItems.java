package uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FurtherEvidenceActionDynamicListItems {
    ISSUE_FURTHER_EVIDENCE("issueFurtherEvidence", "Issue further evidence to all parties"),
    OTHER_DOCUMENT_MANUAL("otherDocumentManual", "Other document type - action manually"),
    INFORMATION_RECEIVED_FOR_INTERLOC_JUDGE("informationReceivedForInterlocJudge", "Information received for Interloc - send to Judge"),
    INFORMATION_RECEIVED_FOR_INTERLOC_TCW("informationReceivedForInterlocTcw", "Information received for Interloc - send to TCW"),
    SEND_TO_INTERLOC_REVIEW_BY_JUDGE("sendToInterlocReviewByJudge", "Send to Interloc - Review by Judge"),
    SEND_TO_INTERLOC_REVIEW_BY_TCW("sendToInterlocReviewByTcw", "Send to Interloc - Review by Tcw"),
    ADMIN_ACTION_CORRECTION("adminActionCorrection", "Admin action correction");

    final String code;
    final String label;
}
