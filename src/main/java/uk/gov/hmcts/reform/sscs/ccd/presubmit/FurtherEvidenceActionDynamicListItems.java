package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import lombok.Getter;

@Getter
public enum FurtherEvidenceActionDynamicListItems {
    ISSUE_FURTHER_EVIDENCE("issueFurtherEvidence", "Issue further evidence to all parties"),
    OTHER_DOCUMENT_MANUAL("otherDocumentManual", "Other document type - action manually"),
    INFORMATION_RECEIVED_FOR_INTERLOC("informationReceivedForInterloc", "Information received for interlocutory review");

    String code;
    String label;

    FurtherEvidenceActionDynamicListItems(String code, String label) {
        this.code = code;
        this.label = label;
    }
}
