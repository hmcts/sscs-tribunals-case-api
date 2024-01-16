package uk.gov.hmcts.reform.sscs.service;

import static java.util.Objects.isNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.DwpDocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.AppConstants;

@Service
public class DwpDocumentService {
    private static final DateTimeFormatter DD_MM_YYYY_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public void addToDwpDocuments(SscsCaseData sscsCaseData, DwpResponseDocument dwpDocument, DwpDocumentType docType) {

        addToDwpDocumentsWithEditedDoc(sscsCaseData, dwpDocument, docType, null, null);
    }

    public void validateEditedEvidenceReason(SscsCaseData sscsCaseData,
                                              PreSubmitCallbackResponse<SscsCaseData> preSubmitCallbackResponse,
                                              String editedEvidenceReason) {

        if (sscsCaseData.isBenefitType(CHILD_SUPPORT) && editedEvidenceReason != null
                && editedEvidenceReason.equals("phme")) {
            preSubmitCallbackResponse
                    .addError("Potential harmful evidence is not a valid selection for child support cases");
        }

        if (!sscsCaseData.isBenefitType(CHILD_SUPPORT) && !isBenefitTypeSscs5(sscsCaseData.getBenefitType()) && editedEvidenceReason != null
                && editedEvidenceReason.equals("childSupportConfidentiality")) {
            preSubmitCallbackResponse
                    .addError("Child support - Confidentiality is not a valid selection for this case");
        }
    }

    private boolean isBenefitTypeSscs5(Optional<Benefit> benefitType) {
        return benefitType.filter(benefit -> SscsType.SSCS5.equals(benefit.getSscsType())).isPresent();
    }


    public void addToDwpDocumentsWithEditedDoc(SscsCaseData sscsCaseData, DwpResponseDocument dwpDocument, DwpDocumentType docType, DocumentLink editedDocumentLink, String editedReason) {


        if (dwpDocument != null) {
            String documentFileName = dwpDocument.getDocumentFileName() != null ? dwpDocument.getDocumentFileName() : docType.getLabel();

            DwpDocumentDetails dwpDocumentDetails = new DwpDocumentDetails(docType.getValue(),
                    documentFileName,
                    null,
                    LocalDateTime.now(),
                    dwpDocument.getDocumentLink(), editedDocumentLink, editedReason, null, null, null, null, null, null, null, null);

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

    public void moveDocsToCorrectCollection(SscsCaseData sscsCaseData) {
        String todayDate = LocalDate.now().format(DD_MM_YYYY_FORMAT);
        if (isDwpResponseDocumentNotNull(sscsCaseData.getDwpAT38Document())) {
            DwpResponseDocument at38 = buildDwpResponseDocumentWithDate(
                    AppConstants.DWP_DOCUMENT_AT38_FILENAME_PREFIX,
                    todayDate,
                    sscsCaseData.getDwpAT38Document().getDocumentLink());

            addToDwpDocuments(sscsCaseData, at38, DwpDocumentType.AT_38);
            sscsCaseData.setDwpAT38Document(null);
        }

        if (isDwpResponseDocumentNotNull(sscsCaseData.getDwpResponseDocument())) {
            sscsCaseData.setDwpResponseDocument(buildDwpResponseDocumentWithDate(
                    AppConstants.DWP_DOCUMENT_RESPONSE_FILENAME_PREFIX,
                    todayDate,
                    sscsCaseData.getDwpResponseDocument().getDocumentLink()));

            moveDwpResponseDocumentToDwpDocumentCollection(sscsCaseData);
        }

        if (isDwpResponseDocumentNotNull(sscsCaseData.getDwpEvidenceBundleDocument())) {
            sscsCaseData.setDwpEvidenceBundleDocument(buildDwpResponseDocumentWithDate(
                    AppConstants.DWP_DOCUMENT_EVIDENCE_FILENAME_PREFIX,
                    todayDate,
                    sscsCaseData.getDwpEvidenceBundleDocument().getDocumentLink()));

            moveDwpEvidenceBundleToDwpDocumentCollection(sscsCaseData);
        }

        if (isDwpResponseDocumentNotNull(sscsCaseData.getAppendix12Doc())) {
            DwpResponseDocument appendix12 = buildDwpResponseDocumentWithDate(
                    AppConstants.DWP_DOCUMENT_APPENDIX12_FILENAME_PREFIX,
                    todayDate,
                    sscsCaseData.getAppendix12Doc().getDocumentLink());

            addToDwpDocuments(sscsCaseData, appendix12, DwpDocumentType.APPENDIX_12);
            sscsCaseData.setAppendix12Doc(null);
        }
    }

    private boolean isDwpResponseDocumentNotNull(DwpResponseDocument dwpResponseDocument) {
        return dwpResponseDocument != null && dwpResponseDocument.getDocumentLink() != null;
    }

    private DwpResponseDocument buildDwpResponseDocumentWithDate(String documentType, String dateForFile, DocumentLink documentLink) {

        if (documentLink == null || documentLink.getDocumentFilename() == null) {
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

    public void removeOldDwpDocuments(SscsCaseData sscsCaseData) {
        sscsCaseData.setDwpAT38Document(null);
        sscsCaseData.setDwpResponseDocument(null);
        sscsCaseData.setDwpEditedResponseDocument(null);
        sscsCaseData.setDwpEvidenceBundleDocument(null);
        sscsCaseData.setDwpEditedEvidenceBundleDocument(null);
        sscsCaseData.setAppendix12Doc(null);
    }
}
