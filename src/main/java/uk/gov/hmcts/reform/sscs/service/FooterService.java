package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.nonNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import uk.gov.hmcts.reform.sscs.util.PdfRequestUtil;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Component
@Slf4j
public class FooterService extends AbstractFooterService<SscsDocument> {

    @Autowired
    public FooterService(PdfStoreService pdfStoreService, PdfWatermarker alter) {
        super(pdfStoreService, alter);
    }

    public void createFooterAndAddDocToCase(DocumentLink url, SscsCaseData caseData, DocumentType documentType, String dateIssued, LocalDate dateAdded, String overrideFileName, SscsDocumentTranslationStatus documentTranslationStatus) {
        createFooterAndAddDocToCase(url, caseData, documentType, dateIssued, dateAdded, overrideFileName,documentTranslationStatus, true);
    }


    protected SscsDocument createFooterDocument(DocumentLink url, String bundleAddition, String documentFileName,
                                                LocalDate dateAdded, DocumentType documentType, SscsDocumentTranslationStatus documentTranslationStatus) {
        return SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName(documentFileName)
                .documentLink(url)
                .bundleAddition(bundleAddition)
                .documentDateAdded(Optional.ofNullable(dateAdded).orElse(LocalDate.now()).format(DateTimeFormatter.ISO_DATE))
                .documentType(documentType.getValue())
                .documentTranslationStatus(documentTranslationStatus)
                .originalPartySender(getOriginalPartySender(url))
                .build())
                .build();
    }

    private String getOriginalPartySender(DocumentLink url) {
        if (nonNull(url)) {
            String documentFileName = url.getDocumentFilename();

            return nonNull(documentFileName) && documentFileName.endsWith(PdfRequestUtil.POST_HEARING_REQUEST_FILE_SUFFIX) ? "FTA" : null;
        }

        return null;
    }

    public void createFooterAndAddDocToCase(DocumentLink url, SscsCaseData caseData, DocumentType documentType, String dateIssued, LocalDate dateAdded, String overrideFileName, SscsDocumentTranslationStatus documentTranslationStatus, boolean shouldAddDocumentToCaseData) {

        String label = documentType.getLabel() != null ? documentType.getLabel() : documentType.getValue();
        log.info(label + " adding footer appendix document link: {} and caseId {}", url, caseData.getCcdCaseId());
        FooterDetails footerDetails = addFooterToExistingToContentAndCreateNewUrl(url, caseData.getSscsDocument(), documentType, overrideFileName, dateIssued);

        if (nonNull(footerDetails)) {
            SscsDocument sscsDocument = createFooterDocument(footerDetails.getUrl(), footerDetails.getBundleAddition(), footerDetails.getBundleFileName(), dateAdded, documentType, documentTranslationStatus);
            if(shouldAddDocumentToCaseData){
                SscsUtil.addDocumentToCaseDataDocuments(caseData, sscsDocument);
            }
        } else {
            log.info("Could not find {} document for caseId {} so skipping generating footer", label, caseData.getCcdCaseId());
        }
    }
}
