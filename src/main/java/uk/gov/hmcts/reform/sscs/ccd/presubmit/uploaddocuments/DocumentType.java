package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

public enum DocumentType {
    MEDICAL_EVIDENCE("Medical evidence"), OTHER_EVIDENCE("Other evidence"),
    APPELLANT_EVIDENCE("appellantEvidence"), REPRESENTATIVE_EVIDENCE("representativeEvidence");
    private String value;

    public String getValue() {
        return value;
    }

    DocumentType(String value) {
        this.value = value;
    }
}