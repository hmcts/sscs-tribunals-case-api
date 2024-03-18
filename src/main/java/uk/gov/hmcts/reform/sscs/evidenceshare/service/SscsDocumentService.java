package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static java.util.Optional.ofNullable;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.AbstractDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.docmosis.domain.Pdf;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.BulkPrintException;
import uk.gov.hmcts.reform.sscs.evidenceshare.model.PdfDocument;
import uk.gov.hmcts.reform.sscs.helper.PdfHelper;
import uk.gov.hmcts.reform.sscs.service.PdfStoreService;

@Slf4j
@Service
public class SscsDocumentService {
    private final PdfStoreService pdfStoreService;
    private final PdfHelper pdfHelper;

    @Autowired
    public SscsDocumentService(PdfStoreService pdfStoreService, PdfHelper pdfHelper) {
        this.pdfStoreService = pdfStoreService;
        this.pdfHelper = pdfHelper;
    }

    public List<PdfDocument> getPdfsForGivenDocTypeNotIssued(List<? extends AbstractDocument> sscsDocuments, DocumentType documentType, boolean isConfidentialCase, String otherPartyOriginalSenderId) {
        Objects.requireNonNull(sscsDocuments);
        Objects.requireNonNull(documentType);

        return sscsDocuments.stream()
            .filter(doc -> documentType.getValue().equals(doc.getValue().getDocumentType())
                && "No".equals(doc.getValue().getEvidenceIssued()))
            .filter(doc -> otherPartyOriginalSenderId == null || (otherPartyOriginalSenderId != null && otherPartyOriginalSenderId.equals(doc.getValue().getOriginalSenderOtherPartyId())))
            .map(doc -> PdfDocument.builder().pdf(toPdf(doc, isConfidentialCase)).document(doc).build())
            .collect(Collectors.toList());
    }

    private Pdf toPdf(AbstractDocument sscsDocument, boolean isConfidentialCase) {
        return new Pdf(getContentForGivenDoc(sscsDocument, isConfidentialCase), sscsDocument.getValue().getDocumentFileName());
    }

    private byte[] getContentForGivenDoc(AbstractDocument sscsDocument, boolean isConfidentialCase) {
        final DocumentLink documentLink = isConfidentialCase ? ofNullable(sscsDocument.getValue().getEditedDocumentLink())
            .orElse(sscsDocument.getValue().getDocumentLink()) : sscsDocument.getValue().getDocumentLink();
        return pdfStoreService.download(documentLink.getDocumentUrl());
    }

    public void filterByDocTypeAndApplyAction(List<SscsDocument> sscsDocument, DocumentType documentType,
                                              Consumer<SscsDocument> action) {
        Objects.requireNonNull(sscsDocument);
        Objects.requireNonNull(documentType);
        Objects.requireNonNull(action);
        sscsDocument.stream()
            .filter(doc -> documentType.getValue().equals(doc.getValue().getDocumentType()))
            .forEach(action);
    }

    public List<PdfDocument> sizeNormalisePdfs(List<PdfDocument> pdfDocuments) {

        List<PdfDocument> normalisedPdfs = new ArrayList<>();

        for (PdfDocument pdfDoc : pdfDocuments) {

            AbstractDocument updatedSscsDocument;
            Pdf updatedPdf;

            Optional<Pdf> resized = resizedPdf(pdfDoc.getPdf());

            if (resized.isPresent()) {
                updatedPdf = resized.get();
                updatedSscsDocument = saveAndUpdateDocument(updatedPdf, pdfDoc.getDocument());
            } else {
                updatedPdf = pdfDoc.getPdf();
                updatedSscsDocument = pdfDoc.getDocument();
            }
            normalisedPdfs.add(PdfDocument.builder().document(updatedSscsDocument).pdf(updatedPdf).build());
        }
        return normalisedPdfs;
    }

    public AbstractDocument saveAndUpdateDocument(Pdf pdf, AbstractDocument document) {

        String pdfFileName = document.getValue().getDocumentFileName() + ".pdf";

        log.info("About to upload resized document [" + pdfFileName + "]");

        try {
            SscsDocument sscsDocument = pdfStoreService.storeDocument(pdf.getContent(), pdfFileName, null);
            String location = sscsDocument.getValue().getDocumentLink().getDocumentUrl();
            DocumentLink documentLink = DocumentLink.builder().documentUrl(location).build();
            document.getValue().setResizedDocumentLink(documentLink);

        } catch (Exception e) {
            log.error("Failed to store resized pdf document but carrying on [" + pdfFileName + "]", e);
        }
        return document;
    }

    public Optional<Pdf> resizedPdf(Pdf originalPdf) throws BulkPrintException {

        try (PDDocument document = PDDocument.load(originalPdf.getContent())) {
            Optional<PDDocument> resizedDoc = pdfHelper.scaleToA4(document);

            if (resizedDoc.isPresent()) {
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    try (PDDocument resized = resizedDoc.get()) {
                        resized.save(baos);
                        return Optional.of(new Pdf(baos.toByteArray(), originalPdf.getName()));
                    }
                }
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new BulkPrintException("Failed to check and resize PDF", e);
        }
    }
}
