package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.lang3.StringUtils.equalsAnyIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.ScannedDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsFurtherEvidenceDoc;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;

@Service
public class UploadDocumentFurtherEvidenceHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        boolean canBeHandled = callbackType != null && callback != null
            && callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent().equals(EventType.UPLOAD_DOCUMENT_FURTHER_EVIDENCE);
        if (!canBeHandled && callback != null) {
            initDraftSscsFurtherEvidenceDocument(callback.getCaseDetails().getCaseData());
        }
        return canBeHandled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        if (!validDraftFurtherEvidenceDocument(caseData.getDraftSscsFurtherEvidenceDocument())) {
            initDraftSscsFurtherEvidenceDocument(caseData);
            PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);
            response.addError("You need to provide a file and a document type");
            return response;
        }
        if (!isFileUploadedAPdf(caseData.getDraftSscsFurtherEvidenceDocument())) {
            initDraftSscsFurtherEvidenceDocument(caseData);
            PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);
            response.addError("You need to upload PDF documents only");
            return response;
        }

        moveDraftsToSscsDocs(caseData);
        initDraftSscsFurtherEvidenceDocument(caseData);
        caseData.setEvidenceHandled("No");
        caseData.setDwpState(DwpState.FE_RECEIVED.getId());

        return new PreSubmitCallbackResponse<>(caseData);
    }

    private boolean validDraftFurtherEvidenceDocument(List<SscsFurtherEvidenceDoc> draftSscsFurtherEvidenceDocuments) {
        if (draftSscsFurtherEvidenceDocuments != null && !draftSscsFurtherEvidenceDocuments.isEmpty()) {
            return draftSscsFurtherEvidenceDocuments.stream()
                .allMatch(doc -> {
                    String docType = doc.getValue() != null ? doc.getValue().getDocumentType() : null;
                    return isValidDocumentType(docType) && isFileUploaded(doc);
                });
        }
        return false;
    }

    private boolean isFileUploadedAPdf(List<SscsFurtherEvidenceDoc> draftSscsFurtherEvidenceDocuments) {
        return draftSscsFurtherEvidenceDocuments.stream().allMatch(this::isFileAPdf);
    }

    private boolean isFileAPdf(SscsFurtherEvidenceDoc doc) {
        return doc.getValue().getDocumentLink() != null
                && isNotBlank(doc.getValue().getDocumentLink().getDocumentUrl())
                && equalsAnyIgnoreCase("pdf", getExtension(doc.getValue().getDocumentLink().getDocumentFilename()));
    }

    private boolean isFileUploaded(SscsFurtherEvidenceDoc doc) {
        return doc.getValue().getDocumentLink() != null
            && isNotBlank(doc.getValue().getDocumentLink().getDocumentUrl());
    }

    private boolean isValidDocumentType(String docType) {
        return DocumentType.MEDICAL_EVIDENCE.getId().equals(docType)
            || DocumentType.OTHER_EVIDENCE.getId().equals(docType)
            || DocumentType.APPELLANT_EVIDENCE.getId().equals(docType)
            || DocumentType.REPRESENTATIVE_EVIDENCE.getId().equals(docType);
    }

    private void initDraftSscsFurtherEvidenceDocument(SscsCaseData caseData) {
        caseData.setDraftSscsFurtherEvidenceDocument(null);
    }

    private void moveDraftsToSscsDocs(SscsCaseData caseData) {
        List<ScannedDocument> newScannedDocs = getNewScannedDocuments(caseData);
        mergeNewScannedDocs(caseData, newScannedDocs);
    }

    private void mergeNewScannedDocs(SscsCaseData caseData, List<ScannedDocument> newScannedDocs) {
        if (caseData.getScannedDocuments() == null) {
            caseData.setScannedDocuments(newScannedDocs);
        } else {
            caseData.getScannedDocuments().addAll(newScannedDocs);
        }
    }

    @NotNull
    private List<ScannedDocument> getNewScannedDocuments(SscsCaseData caseData) {
        List<ScannedDocument> newScannedDocs = new ArrayList<>();
        caseData.getDraftSscsFurtherEvidenceDocument().forEach(draftDoc -> {
            newScannedDocs.add(buildNewScannedDoc(draftDoc));
        });
        return newScannedDocs;
    }

    private ScannedDocument buildNewScannedDoc(SscsFurtherEvidenceDoc draftDoc) {
        return ScannedDocument.builder()
                    .value(ScannedDocumentDetails.builder()
                        .fileName(draftDoc.getValue().getDocumentFileName())
                        .type("other")
                        .scannedDate(LocalDate.now().atStartOfDay().toString())
                        .url(draftDoc.getValue().getDocumentLink())
                        .build())
                    .build();
    }

}
