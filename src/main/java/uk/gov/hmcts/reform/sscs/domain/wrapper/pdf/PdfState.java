package uk.gov.hmcts.reform.sscs.domain.wrapper.pdf;

public enum PdfState {
    OK("ok"),
    UNREADABLE("unreadable"),
    PASSWORD_ENCRYPTED("password_encrypted"),
    UNKNOWN("unknown");

    private final String pdfState;

    PdfState(String pdfState) {
        this.pdfState = pdfState;
    }

    public String getPdfState() {
        return pdfState;
    }

    public static PdfState of(String pdfState) {
        for (PdfState answerState : values()) {
            if (answerState.pdfState.equals(pdfState)) {
                return answerState;
            }
        }
        throw new IllegalArgumentException("No PdfState mapped for [" + pdfState + "]");
    }
}
