package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.DocumentType.REQUEST_FOR_HEARING_RECORDING;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.setHasUnprocessedAudioVideoEvidenceFlag;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.domain.wrapper.pdf.PdfState;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.util.DocumentUtil;

@Service
public class UploadDocumentFurtherEvidenceAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private static final String UPLOAD_DATE_FORMATTER = "yyyy-MM-dd";
    private final boolean uploadAudioVideoEvidenceEnabled;
    private final FooterService footerService;

    @Autowired
    public UploadDocumentFurtherEvidenceAboutToSubmitHandler(
        @Value("${feature.upload-audio-video-evidence.enabled}") boolean uploadAudioVideoEvidenceEnabled,
        FooterService footerService) {
        this.uploadAudioVideoEvidenceEnabled = uploadAudioVideoEvidenceEnabled;
        this.footerService = footerService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType != null && callback != null
            && callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent().equals(EventType.UPLOAD_DOCUMENT_FURTHER_EVIDENCE);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        if (!validDraftFurtherEvidenceDocument(caseData.getDraftSscsFurtherEvidenceDocument())) {
            response.addError("You need to provide a file and a document type");
        } else if (!uploadAudioVideoEvidenceEnabled
            && !isFileUploadedAPdf(caseData.getDraftSscsFurtherEvidenceDocument())) {
            response.addError("You need to upload PDF documents only");
        } else if (uploadAudioVideoEvidenceEnabled
            && !isFileUploadedAValid(caseData.getDraftSscsFurtherEvidenceDocument())) {
            response.addError("You need to upload PDF, MP3 or MP4 file only");
        }

        PdfState pdfState = isPdfReadable(caseData.getDraftSscsFurtherEvidenceDocument());
        switch (pdfState) {
            case UNKNOWN:
            case UNREADABLE:
                initDraftSscsFurtherEvidenceDocument(caseData);
                response.addError("Your PDF Document is not readable.");
                return response;
            case PASSWORD_ENCRYPTED:
                initDraftSscsFurtherEvidenceDocument(caseData);
                response.addError("Your PDF Document cannot be password protected.");
                return response;
            case OK:
            default:
                break;
        }

        moveDraftsToSscsDocs(caseData);
        moveDraftsToAudioVideoEvidence(caseData);
        caseData.setEvidenceHandled("No");
        uploadHearingRecordingRequest(caseData);

        initDraftSscsFurtherEvidenceDocument(caseData);
        setHasUnprocessedAudioVideoEvidenceFlag(caseData);

        return response;
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
        return draftSscsFurtherEvidenceDocuments.stream().allMatch(doc ->
            DocumentUtil.isFileAPdf(doc.getValue().getDocumentLink()));
    }

    private boolean isFileUploadedAValid(List<SscsFurtherEvidenceDoc> draftSscsFurtherEvidenceDocuments) {
        return draftSscsFurtherEvidenceDocuments.stream().allMatch(doc ->
            DocumentUtil.isFileAPdf(doc.getValue().getDocumentLink())
                || DocumentUtil.isFileAMedia(doc.getValue().getDocumentLink()));
    }

    private PdfState isPdfReadable(List<SscsFurtherEvidenceDoc> docs) {
        PdfState pdfState = PdfState.UNKNOWN;
        if (isNotEmpty(docs)) {
            for (SscsFurtherEvidenceDoc doc : docs) {
                if (DocumentUtil.isFileAPdf(doc.getValue().getDocumentLink())) {
                    pdfState = isPdfReadable(doc);
                    if (!PdfState.OK.equals(pdfState)) {
                        return pdfState;
                    }
                } else if (DocumentUtil.isFileAMedia(doc.getValue().getDocumentLink())) {
                    pdfState = PdfState.OK;
                }
            }
        }
        return pdfState;
    }

    private PdfState isPdfReadable(SscsFurtherEvidenceDoc doc) {
        if (doc.getValue().getDocumentLink() != null) {
            return footerService.isReadablePdf(doc.getValue().getDocumentLink().getDocumentUrl());
        }
        return PdfState.UNKNOWN;
    }

    private boolean isFileUploaded(SscsFurtherEvidenceDoc doc) {
        return doc.getValue().getDocumentLink() != null
            && isNotBlank(doc.getValue().getDocumentLink().getDocumentUrl());
    }

    private boolean isValidDocumentType(String docType) {
        return DocumentType.MEDICAL_EVIDENCE.getId().equals(docType)
            || DocumentType.OTHER_EVIDENCE.getId().equals(docType)
            || DocumentType.APPELLANT_EVIDENCE.getId().equals(docType)
            || DocumentType.REPRESENTATIVE_EVIDENCE.getId().equals(docType)
            || DocumentType.REQUEST_FOR_HEARING_RECORDING.getId().equals(docType);
    }

    private void initDraftSscsFurtherEvidenceDocument(SscsCaseData caseData) {
        caseData.setDraftSscsFurtherEvidenceDocument(null);
    }

    private void moveDraftsToSscsDocs(SscsCaseData caseData) {
        List<ScannedDocument> newScannedDocs = getNewScannedDocuments(caseData);
        if (!newScannedDocs.isEmpty()) {
            mergeNewScannedDocs(caseData, newScannedDocs);
        }
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
            .filter(draftDoc -> !REQUEST_FOR_HEARING_RECORDING.getId().equals(draftDoc.getValue().getDocumentType()))
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
                    .fileName(doc.getValue().getDocumentFileName() != null ? doc.getValue().getDocumentFileName() :
                        doc.getValue().getDocumentLink().getDocumentFilename())
                    .dateAdded(LocalDate.now())
                    .partyUploaded(UploadParty.CTSC)
                    .build()).build()).collect(toList());

        if (!newAudioVideoEvidence.isEmpty()) {
            if (sscsCaseData.getAudioVideoEvidence() == null) {
                sscsCaseData.setAudioVideoEvidence(new ArrayList<>());
            }
            sscsCaseData.getAudioVideoEvidence().addAll(newAudioVideoEvidence);
            if (!REVIEW_BY_JUDGE.getId().equals(sscsCaseData.getInterlocReviewState())) {
                sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_TCW.getId());
            }
            sscsCaseData.setInterlocReferralReason(REVIEW_AUDIO_VIDEO_EVIDENCE.getId());
        }
    }

    private void uploadHearingRecordingRequest(SscsCaseData sscsCaseData) {

        if (sscsCaseData.getDraftSscsFurtherEvidenceDocument() != null) {
            List<SscsFurtherEvidenceDoc> sscsFurtherEvidenceDocList = sscsCaseData.getDraftSscsFurtherEvidenceDocument().stream()
                    .filter(draftDoc -> REQUEST_FOR_HEARING_RECORDING.getId().equals(draftDoc.getValue().getDocumentType())).collect(toList());

            if (sscsCaseData.getSscsHearingRecordingCaseData() != null
                && sscsCaseData.getSscsHearingRecordingCaseData().getRequestingParty() != null
                && sscsCaseData.getSscsHearingRecordingCaseData().getRequestingParty().getValue() != null
                && sscsCaseData.getSscsHearingRecordingCaseData().getRequestingParty().getValue().getCode() != null
                && sscsFurtherEvidenceDocList != null && sscsFurtherEvidenceDocList.size() >= 1) {

                HearingRecordingRequest hearingRecordingRequest = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder()
                    .requestingParty(sscsCaseData.getSscsHearingRecordingCaseData().getRequestingParty().getValue().getCode())
                    .requestDocument(sscsFurtherEvidenceDocList.get(0).getValue().getDocumentLink())
                    .dateRequested(LocalDateTime.now().format(DateTimeFormatter.ofPattern(UPLOAD_DATE_FORMATTER))).build()).build();

                List<HearingRecordingRequest> hearingRecordingRequests = sscsCaseData.getSscsHearingRecordingCaseData().getRequestedHearings();
                if (hearingRecordingRequests == null) {
                    hearingRecordingRequests = new ArrayList<>();
                }
                hearingRecordingRequests.add(hearingRecordingRequest);

                sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(hearingRecordingRequests);
                sscsCaseData.getSscsHearingRecordingCaseData().setHearingRecordingRequestOutstanding(YesNo.YES);
            }
        }
    }
}
