package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.isNull;

import java.time.LocalDate;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@Service
public class DwpDocumentService {

    public void addToDwpDocuments(SscsCaseData sscsCaseData, DwpResponseDocument dwpDocument, DwpDocumentType docType) {

        addToDwpDocumentsWithEditedDoc(sscsCaseData, dwpDocument, docType, null);
    }

    public void addToDwpDocumentsWithEditedDoc(SscsCaseData sscsCaseData, DwpResponseDocument dwpDocument, DwpDocumentType docType, DocumentLink editedDocumentLink) {

        if (dwpDocument != null) {
            DwpDocumentDetails dwpDocumentDetails = new DwpDocumentDetails(docType.getValue(),
                    docType.getLabel(),
                    LocalDate.now().toString(),
                    dwpDocument.getDocumentLink(), editedDocumentLink, null, null, null);

            DwpDocument doc = new DwpDocument(dwpDocumentDetails);

            if (isNull(sscsCaseData.getDwpDocuments())) {
                sscsCaseData.setDwpDocuments(new ArrayList<>());
            }
            sscsCaseData.getDwpDocuments().add(doc);
            sscsCaseData.sortCollections();
        }
    }

    public void removeDwpDocumentTypeFromCollection(SscsCaseData sscsCaseData, DwpDocumentType docType) {
        if (null != sscsCaseData.getDwpDocuments()) {
            sscsCaseData.getDwpDocuments().removeIf(e -> docType.getValue().equals(e.getValue().getDocumentType()));
        }
    }
}
