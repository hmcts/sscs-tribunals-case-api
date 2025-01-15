package uk.gov.hmcts.reform.sscs.docmosis.service;

import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;

public interface PdfGenerationService {
    byte[] generatePdf(DocumentHolder documentHolder);
}
