package uk.gov.hmcts.reform.sscs.evidenceshare.exception;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class PdfStoreException extends RuntimeException {
    public static final long serialVersionUID = -7268250396297541580L;

    public PdfStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
