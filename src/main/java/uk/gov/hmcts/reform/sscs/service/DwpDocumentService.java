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

        addToDwpDocumentsWithEditedDoc(sscsCaseData, dwpDocument, docType, null, null);
    }

    public void addToDwpDocumentsWithEditedDoc(SscsCaseData sscsCaseData, DwpResponseDocument dwpDocument, DwpDocumentType docType, DocumentLink editedDocumentLink, String editedReason) {


        if (dwpDocument != null) {
            String documentFileName = dwpDocument.getDocumentFileName() != null ? dwpDocument.getDocumentFileName() : docType.getLabel();

            DwpDocumentDetails dwpDocumentDetails = new DwpDocumentDetails(docType.getValue(),
                    documentFileName,
                    LocalDate.now().toString(),
                    dwpDocument.getDocumentLink(), editedDocumentLink, editedReason, null, null, null);

            DwpDocument doc = new DwpDocument(dwpDocumentDetails);

            if (isNull(sscsCaseData.getDwpDocuments())) {
                sscsCaseData.setDwpDocuments(new ArrayList<>());
            }
            sscsCaseData.getDwpDocuments().add(doc);
            sscsCaseData.sortCollections();
        }
    }

    public void moveDwpResponseDocumentToDwpDocumentCollection(SscsCaseData sscsCaseData) {
        removeDwpDocumentTypeFromCollection(sscsCaseData, DwpDocumentType.DWP_RESPONSE);
        DocumentLink editedResponseDocumentLink = sscsCaseData.getDwpEditedResponseDocument() != null ? sscsCaseData.getDwpEditedResponseDocument().getDocumentLink() : null;
        addToDwpDocumentsWithEditedDoc(sscsCaseData, sscsCaseData.getDwpResponseDocument(), DwpDocumentType.DWP_RESPONSE, editedResponseDocumentLink, sscsCaseData.getDwpEditedEvidenceReason());
        sscsCaseData.setDwpResponseDocument(null);
        sscsCaseData.setDwpEditedResponseDocument(null);
    }

    public void moveDwpEvidenceBundleToDwpDocumentCollection(SscsCaseData sscsCaseData) {
        removeDwpDocumentTypeFromCollection(sscsCaseData, DwpDocumentType.DWP_EVIDENCE_BUNDLE);
        DocumentLink editedEvidenceBundleDocumentLink = sscsCaseData.getDwpEditedEvidenceBundleDocument() != null ? sscsCaseData.getDwpEditedEvidenceBundleDocument().getDocumentLink() : null;
        addToDwpDocumentsWithEditedDoc(sscsCaseData, sscsCaseData.getDwpEvidenceBundleDocument(), DwpDocumentType.DWP_EVIDENCE_BUNDLE, editedEvidenceBundleDocumentLink, sscsCaseData.getDwpEditedEvidenceReason());
        sscsCaseData.setDwpEvidenceBundleDocument(null);
        sscsCaseData.setDwpEditedEvidenceBundleDocument(null);
    }

    protected void removeDwpDocumentTypeFromCollection(SscsCaseData sscsCaseData, DwpDocumentType docType) {
        if (null != sscsCaseData.getDwpDocuments()) {
            sscsCaseData.getDwpDocuments().removeIf(e -> docType.getValue().equals(e.getValue().getDocumentType()));
        }
    }
}
