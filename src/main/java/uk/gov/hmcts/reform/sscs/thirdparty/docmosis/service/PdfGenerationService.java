package uk.gov.hmcts.reform.sscs.thirdparty.docmosis.service;

import uk.gov.hmcts.reform.sscs.thirdparty.docmosis.domain.DocumentHolder;

public interface PdfGenerationService {
    byte[] generatePdf(DocumentHolder documentHolder);
}
