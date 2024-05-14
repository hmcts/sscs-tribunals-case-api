package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.docmosis.domain.DocumentHolder;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Template;
import uk.gov.hmcts.reform.sscs.docmosis.service.PdfGenerationService;
import uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.UnableToContactThirdPartyException;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.FurtherEvidencePlaceholderService;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;

@Service
@Slf4j
public class CoverLetterService {
    private FurtherEvidencePlaceholderService furtherEvidencePlaceholderService;

    private PdfGenerationService pdfGenerationService;

    private PdfStoreService pdfStoreService;

    private int maxRetryAttempts;

    @Autowired
    public CoverLetterService(FurtherEvidencePlaceholderService furtherEvidencePlaceholderService,
                              PdfStoreService pdfStoreService,
                              @Qualifier("docmosisPdfGenerationService") PdfGenerationService pdfGenerationService,
                              @Value("${send-letter.maxRetryAttempts:3}") int maxRetryAttempts) {
        this.furtherEvidencePlaceholderService = furtherEvidencePlaceholderService;
        this.pdfStoreService = pdfStoreService;
        this.pdfGenerationService = pdfGenerationService;
        this.maxRetryAttempts = maxRetryAttempts;
    }

    public void appendCoverLetter(byte[] coverLetterContent, List<Pdf> pdfsToBulkPrint, String pdfName) {
        requireNonNull(coverLetterContent, "coverLetter must not be null");
        requireNonNull(pdfsToBulkPrint, "pdfsToBulkPrint must not be null");
        Pdf pdfCoverLetter = new Pdf(coverLetterContent, pdfName);

        pdfsToBulkPrint.add(0, pdfCoverLetter);
    }

    /**
     * Intended use of this method is only for local development or testing. This method produces a
     * cover letter pdf file and stores it in the application root path.
     *
     * @param coverLetterContent this is the content in bytes of the cover letter from docmosis
     */
    private void printCoverLetterToPdfLocallyForDebuggingPurpose(byte[] coverLetterContent,
                                                                 FurtherEvidenceLetterType letterType,
                                                                 String hmctsDocName) {
        if (log.isDebugEnabled()) {
            try {
                FileUtils.writeByteArrayToFile(new File(hmctsDocName + letterType.getValue()
                    + "CoverLetter" + ".pdf"), coverLetterContent);
            } catch (Exception e) {
                log.info("CoverLetter fails to be created", e);
            }
        }
    }

    public byte[] generateCoverLetter(SscsCaseData caseData, FurtherEvidenceLetterType letterType, String templateName,
                                      String hmctsDocName, String otherPartyId) {

        requireNonNull(caseData, "caseData must not be null");

        Map<String, Object> placeholders = furtherEvidencePlaceholderService.populatePlaceholders(caseData, letterType, otherPartyId);
        return generateCoverLetterRetry(letterType, templateName, hmctsDocName, placeholders, 1);
    }

    public byte[] generateCoverLetterRetry(FurtherEvidenceLetterType letterType, String templateName,
                                           String hmctsDocName, Map<String, Object> placeholders, int retries) {
        try {
            byte[] coverLetterContent = pdfGenerationService.generatePdf(DocumentHolder.builder()
                .template(new Template(templateName, hmctsDocName))
                .placeholders(placeholders)
                .pdfArchiveMode(true)
                .build());

            printCoverLetterToPdfLocallyForDebuggingPurpose(coverLetterContent, letterType, hmctsDocName);

            return coverLetterContent;

        } catch (Exception e) {
            if (retries < maxRetryAttempts) {
                log.info("Retrying Cover Letter Service retry " + retries + " due to " + e.getMessage());
                return generateCoverLetterRetry(letterType, templateName, hmctsDocName, placeholders, retries + 1);
            } else {
                throw new UnableToContactThirdPartyException("docmosis", e);
            }
        }
    }

    public byte[] generateCoverSheet(String templateName, String hmctsDocName, Map<String, Object> placeholders) {
        byte[] coverSheetContent = pdfGenerationService.generatePdf(DocumentHolder.builder()
            .template(new Template(templateName, hmctsDocName))
            .placeholders(placeholders)
            .pdfArchiveMode(true)
            .build());

        printCoverLetterToPdfLocallyForDebuggingPurpose(coverSheetContent, FurtherEvidenceLetterType.APPELLANT_LETTER, hmctsDocName);

        return coverSheetContent;
    }

    public List<Pdf> getSelectedDocuments(SscsCaseData sscsCaseData) {
        List<Pdf> documents = new ArrayList<>();

        for (CcdValue<DocumentSelectionDetails> d : sscsCaseData.getDocumentSelection()) {
            var documentLink = findDocumentByFileName(d.getValue().getDocumentsList().getValue().getCode(), sscsCaseData);
            byte[] document = null;

            if (documentLink != null) {
                document = pdfStoreService.download(documentLink.getDocumentUrl());
                Pdf pdf = new Pdf(document, documentLink.getDocumentFilename());
                documents.add(pdf);
            }

        }
        return documents;
    }

    private DocumentLink findDocumentByFileName(String fileName, SscsCaseData sscsCaseData) {
        DocumentLink dwpLink = searchDocumentsForFile(sscsCaseData.getDwpDocuments(), fileName);
        if (dwpLink != null) {
            return dwpLink;
        } else {
            return searchDocumentsForFile(sscsCaseData.getSscsDocument(), fileName);
        }
    }

    private DocumentLink searchDocumentsForFile(List<? extends AbstractDocument> documents, String fileName) {
        if (isNotEmpty(documents)) {
            return documents.stream()
                .map(document -> {
                    if (fileName.equals(document.getValue().getDocumentFileName())) {
                        return document.getValue().getDocumentLink();
                    } else if (document.getValue().getEditedDocumentLink() != null
                        && fileName.equals(document.getValue().getEditedDocumentLink().getDocumentFilename())) {
                        return document.getValue().getEditedDocumentLink();
                    }
                    return DocumentLink.builder().build();
                })
                .filter(documentLink -> documentLink.getDocumentUrl() != null)
                .findAny().orElse(null);
        }
        return null;
    }
}
