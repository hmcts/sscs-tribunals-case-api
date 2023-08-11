package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.nonNull;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.pdf.PdfWatermarker;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Component
@Slf4j
public class FooterService extends AbstractFooterService<SscsDocument> {

    @Autowired
    public FooterService(PdfStoreService pdfStoreService, PdfWatermarker alter) {
        super(pdfStoreService, alter);
    }

    public void createFooterAndAddDocToCase(DocumentLink url, SscsCaseData caseData, DocumentType documentType, String dateIssued, LocalDate dateAdded,
                                            String overrideFileName, SscsDocumentTranslationStatus documentTranslationStatus) {
        createFooterAndAddDocToCase(url, caseData, documentType, dateIssued, dateAdded, overrideFileName, documentTranslationStatus, null);
    }

    public void createFooterAndAddDocToCase(DocumentLink url, SscsCaseData caseData, DocumentType documentType, String dateIssued, LocalDate dateAdded,
                                            String overrideFileName, SscsDocumentTranslationStatus documentTranslationStatus, EventType eventType) {

        String label = documentType.getLabel() != null ? documentType.getLabel() : documentType.getValue();
        log.info(label + " adding footer appendix document link: {} and caseId {}", url, caseData.getCcdCaseId());
        FooterDetails footerDetails = addFooterToExistingToContentAndCreateNewUrl(url, caseData.getSscsDocument(), documentType, overrideFileName, dateIssued);

        if (nonNull(footerDetails)) {
            SscsDocument sscsDocument = createFooterDocument(footerDetails.getUrl(), footerDetails.getBundleAddition(), footerDetails.getBundleFileName(),
                dateAdded, documentType, documentTranslationStatus, eventType);
            SscsUtil.addDocumentToCaseDataDocuments(caseData, sscsDocument);
        } else {
            log.info("Could not find {} document for caseId {} so skipping generating footer", label, caseData.getCcdCaseId());
        }
    }


    protected SscsDocument createFooterDocument(DocumentLink url, String bundleAddition, String documentFileName,
                                                LocalDate dateAdded, DocumentType documentType, SscsDocumentTranslationStatus documentTranslationStatus,
                                                EventType eventType) {
        return SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName(documentFileName)
                .documentLink(url)
                .bundleAddition(bundleAddition)
                .documentDateAdded(Optional.ofNullable(dateAdded).orElse(LocalDate.now()).format(DateTimeFormatter.ISO_DATE))
                .documentType(documentType.getValue())
                .documentTranslationStatus(documentTranslationStatus)
                .originalPartySender(getOriginalPartySender(eventType))
                .build())
                .build();
    }

    private String getOriginalPartySender(EventType eventType) {
        if (eventType != null) {
            switch (eventType) {
                case POST_HEARING_REQUEST:
                    return "FTA";
                case SEND_TO_FIRST_TIER:
                    return "Upper Tribunal";
                default:
                    return null;
            }
        } else {
            return null;
        }
    }
}
