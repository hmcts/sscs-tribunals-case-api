package uk.gov.hmcts.reform.sscs.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.pdf.PdfWatermarker;

@Component
@Slf4j
public class FooterService extends AbstractFooterService<SscsDocument> {

    @Autowired
    public FooterService(PdfStoreService pdfStoreService, PdfWatermarker alter) {
        super(pdfStoreService, alter);
    }

    public void createFooterAndAddDocToCase(DocumentLink url, SscsCaseData caseData, DocumentType documentType, String dateIssued, LocalDate dateAdded, String overrideFileName, SscsDocumentTranslationStatus documentTranslationStatus) {

        String label = documentType.getLabel() != null ? documentType.getLabel() : documentType.getValue();
        log.info(label + " adding footer appendix document link: {} and caseId {}", url, caseData.getCcdCaseId());
        FooterDetails footerDetails = addFooterToExistingToContentAndCreateNewUrl(url, caseData.getSscsDocument(), documentType, overrideFileName, dateIssued);

        if (footerDetails != null) {
            SscsDocument sscsDocument = createFooterDocument(footerDetails.getUrl(), label, footerDetails.getBundleAddition(), footerDetails.getBundleFileName(), dateAdded, documentType, documentTranslationStatus);
            addDocumentToCaseDataDocuments(caseData, sscsDocument);
        } else {
            log.info("Could not find {} document for caseId {} so skipping generating footer", label, caseData.getCcdCaseId());
        }
    }


    protected SscsDocument createFooterDocument(DocumentLink url, String leftText, String bundleAddition, String documentFileName,
                                                LocalDate dateAdded, DocumentType documentType, SscsDocumentTranslationStatus documentTranslationStatus) {
        return SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName(documentFileName)
                .documentLink(url)
                .bundleAddition(bundleAddition)
                .documentDateAdded(Optional.ofNullable(dateAdded).orElse(LocalDate.now()).format(DateTimeFormatter.ISO_DATE))
                .documentType(documentType.getValue())
                .documentTranslationStatus(documentTranslationStatus)
                .build())
                .build();
    }

    protected void addDocumentToCaseDataDocuments(SscsCaseData caseData, SscsDocument sscsDocument) {
        List<SscsDocument> documents = new ArrayList<>();
        documents.add(sscsDocument);

        if (caseData.getSscsDocument() != null) {
            documents.addAll(caseData.getSscsDocument());
        }
        caseData.setSscsDocument(documents);
    }
}
