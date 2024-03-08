package uk.gov.hmcts.reform.sscs.exception.evidenceshare;

@SuppressWarnings("squid:MaximumInheritanceDepth")
public class PdfStoreException extends RuntimeException {
    public static final long serialVersionUID = -7268250396297541580L;

    public PdfStoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
