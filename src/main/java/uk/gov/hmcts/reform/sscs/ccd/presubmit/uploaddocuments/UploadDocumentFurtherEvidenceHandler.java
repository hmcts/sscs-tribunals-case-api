package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.util.DocumentUtil;

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
        if (!isFileUploadedAValid(caseData.getDraftSscsFurtherEvidenceDocument())) {
            initDraftSscsFurtherEvidenceDocument(caseData);
            PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);
            response.addError("You need to upload PDF,MP3 or MP4 file only");
            return response;
        }

        moveDraftsToSscsDocs(caseData);
        moveDraftsToAudioVideoEvidence(caseData);
        initDraftSscsFurtherEvidenceDocument(caseData);
        caseData.setEvidenceHandled("No");

        if (!State.WITH_DWP.equals(callback.getCaseDetails().getState())) {
            caseData.setDwpState(DwpState.FE_RECEIVED.getId());
        }

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

    private boolean isFileUploadedAValid(List<SscsFurtherEvidenceDoc> draftSscsFurtherEvidenceDocuments) {
        return draftSscsFurtherEvidenceDocuments.stream().allMatch(doc ->
                DocumentUtil.isFileAPdf(doc.getValue().getDocumentLink())
                        || DocumentUtil.isFileAMedia(doc.getValue().getDocumentLink()));
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
        caseData.getDraftSscsFurtherEvidenceDocument().stream()
                .filter(draftDoc -> DocumentUtil.isFileAPdf(draftDoc.getValue().getDocumentLink()))
                .forEach(draftDoc -> {
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

    private void moveDraftsToAudioVideoEvidence(SscsCaseData sscsCaseData) {
        List<AudioVideoEvidence> newAudioVideoEvidence = sscsCaseData.getDraftSscsFurtherEvidenceDocument().stream()
                .filter(doc -> DocumentUtil.isFileAMedia(doc.getValue().getDocumentLink()))
                .map(doc ->
                        AudioVideoEvidence.builder().value(AudioVideoEvidenceDetails.builder()
                                .documentLink(doc.getValue().getDocumentLink())
                                .fileName(doc.getValue().getDocumentFileName())
                                .documentType(doc.getValue().getDocumentType())
                                .dateAdded(LocalDate.now())
                                .build()).build()).collect(toList());

        if (!newAudioVideoEvidence.isEmpty()) {
            if (sscsCaseData.getAudioVideoEvidence() == null) {
                sscsCaseData.setAudioVideoEvidence(new ArrayList<>());
            }
            sscsCaseData.getAudioVideoEvidence().addAll(newAudioVideoEvidence);
            sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_TCW.getId());
        }
    }
}
