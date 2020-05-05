package uk.gov.hmcts.reform.sscs.thirdparty.pdfservice;

public interface PdfService {
    byte[] createPdf(Object pdfSummary, String templatePath);
}
