package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.isNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.AppConstants;

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
                    null,
                    LocalDateTime.now(),
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

    public void moveDocsToCorrectCollection(SscsCaseData sscsCaseData, String todayDate) {
        if (sscsCaseData.getDwpAT38Document() != null) {
            DwpResponseDocument at38 = buildDwpResponseDocumentWithDate(
                    AppConstants.DWP_DOCUMENT_AT38_FILENAME_PREFIX,
                    todayDate,
                    sscsCaseData.getDwpAT38Document().getDocumentLink());

            addToDwpDocuments(sscsCaseData, at38, DwpDocumentType.AT_38);
            sscsCaseData.setDwpAT38Document(null);
        }

        if (sscsCaseData.getDwpResponseDocument() != null) {
            sscsCaseData.setDwpResponseDocument(buildDwpResponseDocumentWithDate(
                    AppConstants.DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX,
                    todayDate,
                    sscsCaseData.getDwpResponseDocument().getDocumentLink()));

            moveDwpResponseDocumentToDwpDocumentCollection(sscsCaseData);
        }

        if (sscsCaseData.getDwpEvidenceBundleDocument() != null) {
            sscsCaseData.setDwpEvidenceBundleDocument(buildDwpResponseDocumentWithDate(
                    AppConstants.DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX,
                    todayDate,
                    sscsCaseData.getDwpEvidenceBundleDocument().getDocumentLink()));

            moveDwpEvidenceBundleToDwpDocumentCollection(sscsCaseData);
        }

        if (sscsCaseData.getAppendix12Doc() != null && sscsCaseData.getAppendix12Doc().getDocumentLink() != null) {
            DwpResponseDocument appendix12 = buildDwpResponseDocumentWithDate(
                    AppConstants.DWP_DOCUMENT_APPENDIX12_FILENAME_PREFIX,
                    todayDate,
                    sscsCaseData.getAppendix12Doc().getDocumentLink());

            addToDwpDocuments(sscsCaseData, appendix12, DwpDocumentType.APPENDIX_12);
            sscsCaseData.setAppendix12Doc(null);
        }
    }

    private DwpResponseDocument buildDwpResponseDocumentWithDate(String documentType, String dateForFile, DocumentLink documentLink) {

        if (documentLink.getDocumentFilename() == null) {
            return null;
        }

        String fileExtension = documentLink.getDocumentFilename().substring(documentLink.getDocumentFilename().lastIndexOf("."));
        return (DwpResponseDocument.builder()
                .documentFileName(documentType + " on " + dateForFile)
                .documentLink(
                        DocumentLink.builder()
                                .documentBinaryUrl(documentLink.getDocumentBinaryUrl())
                                .documentUrl(documentLink.getDocumentUrl())
                                .documentFilename(documentType + " on " + dateForFile + fileExtension)
                                .build()
                ).build());
    }
}
