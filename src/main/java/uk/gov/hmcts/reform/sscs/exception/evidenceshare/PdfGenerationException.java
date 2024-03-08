package uk.gov.hmcts.reform.sscs.exception.evidenceshare;

public class PdfGenerationException extends RuntimeException {

    public PdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
