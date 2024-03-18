package uk.gov.hmcts.reform.sscs.evidenceshare.exception;

public class NoDl6DocumentException extends RuntimeException {
    public static final long serialVersionUID = 1L;

    public NoDl6DocumentException() {
        super("Triggered from Evidence Share – no DL6/16 present, please validate.");
    }
}
