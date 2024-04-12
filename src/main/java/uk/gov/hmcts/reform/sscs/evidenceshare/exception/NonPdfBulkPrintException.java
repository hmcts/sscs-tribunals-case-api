package uk.gov.hmcts.reform.sscs.evidenceshare.exception;

public class NonPdfBulkPrintException extends RuntimeException {
    public static final long serialVersionUID = 1052009977533420152L;

    public NonPdfBulkPrintException(Throwable exception) {
        super("Non-PDFs/broken PDFs seen in list of documents, please correct.", exception);
    }

}
