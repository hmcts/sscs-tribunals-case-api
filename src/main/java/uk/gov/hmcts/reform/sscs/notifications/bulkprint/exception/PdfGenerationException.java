package uk.gov.hmcts.reform.sscs.notifications.bulkprint.exception;

public class PdfGenerationException extends RuntimeException {

    public PdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
