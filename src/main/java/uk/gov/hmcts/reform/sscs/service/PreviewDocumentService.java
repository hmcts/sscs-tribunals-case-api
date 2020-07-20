package uk.gov.hmcts.reform.sscs.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;

@Service
public class PreviewDocumentService {

    public void writePreviewDocumentToSscsDocument(SscsCaseData sscsCaseData, DocumentType documentType, DocumentLink documentLink) {
        if (sscsCaseData.getSscsDocument() != null) {
            sscsCaseData.getSscsDocument()
                    .removeIf(doc -> documentType.getValue().equals(doc.getValue().getDocumentType()));
        }

        SscsDocument draftDecisionNotice = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentFileName(createFileName(documentType))
                .documentLink(documentLink)
                .documentDateAdded(setDocumentDateAdded())
                .documentType(documentType.getValue())
                .build()).build();

        List<SscsDocument> documents = new ArrayList<>();

        documents.add(draftDecisionNotice);

        if (sscsCaseData.getSscsDocument() != null) {
            documents.addAll(sscsCaseData.getSscsDocument());
        }
        sscsCaseData.setSscsDocument(documents);
    }

    private String createFileName(DocumentType documentType) {
        return String.format("%s generated on %s.pdf", documentType.getLabel(), LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
    }

    private String setDocumentDateAdded() {
        return LocalDate.now().format(DateTimeFormatter.ISO_DATE);
    }
}
