package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionfurtherevidence;

public enum DocumentType {
    OTHER_DOCUMENT("Other Document");
    private String value;

    public String getValue() {
        return value;
    }

    DocumentType(String value) {
        this.value = value;
    }
}