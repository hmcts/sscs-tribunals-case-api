package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence;

import lombok.Getter;

@Getter
public enum DocumentType {
    OTHER_DOCUMENT("otherDocument"), APPELLANT_EVIDENCE("appellantEvidence"),
    REPRESENTATIVE_EVIDENCE("representativeEvidence");
    private String value;

    DocumentType(String value) {
        this.value = value;
    }
}