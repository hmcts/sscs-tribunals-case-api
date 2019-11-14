package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

public enum DocumentType {
    MEDICAL_EVIDENCE("Medical evidence"), OTHER_EVIDENCE("Other evidence"),
    APPELLANT_EVIDENCE("appellantEvidence"), REPRESENTATIVE_EVIDENCE("representativeEvidence"),
    TL1_FORM("tl1Form");
    private String id;

    public String getId() {
        return id;
    }

    DocumentType(String id) {
        this.id = id;
    }
}