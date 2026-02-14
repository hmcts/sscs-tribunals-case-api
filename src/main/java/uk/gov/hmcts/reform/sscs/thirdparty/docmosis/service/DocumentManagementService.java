package uk.gov.hmcts.reform.sscs.thirdparty.docmosis.service;

import lombok.extern.slf4j.Slf4j;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.service.CcdPdfService;
import uk.gov.hmcts.reform.sscs.thirdparty.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.thirdparty.docmosis.domain.Pdf;

@Slf4j
public class DocumentManagementService {

    private final PdfGenerationService pdfGenerationService;

    private final CcdPdfService ccdPdfService;

    private final IdamService idamService;

    public DocumentManagementService(PdfGenerationService pdfGenerationService,
                                     CcdPdfService ccdPdfService,
                                     IdamService idamService) {
        this.pdfGenerationService = pdfGenerationService;
        this.ccdPdfService = ccdPdfService;
        this.idamService = idamService;

    }

    public Pdf generateDocumentAndAddToCcd(DocumentHolder holder, SscsCaseData caseData) {
        log.info("Generating template {} for case id {}", holder.getTemplate().getHmctsDocName(), caseData.getCcdCaseId());

        byte[] pdfBytes = pdfGenerationService.generatePdf(holder);
        String pdfName = getPdfName(holder.getTemplate().getHmctsDocName(), caseData.getCcdCaseId());

        log.info("Adding document template {} to ccd for id {}", holder.getTemplate().getHmctsDocName(), caseData.getCcdCaseId());
        String description = "Uploaded " + pdfName + " into SSCS";
        ccdPdfService.mergeDocIntoCcd(pdfName, pdfBytes, Long.valueOf(caseData.getCcdCaseId()), caseData, idamService.getIdamTokens(), description, holder.getTemplate().getHmctsDocName());

        return new Pdf(pdfBytes, pdfName);
    }

    private String getPdfName(String documentNamePrefix, String caseId) {
        return documentNamePrefix + "-" + caseId + ".pdf";
    }
}
