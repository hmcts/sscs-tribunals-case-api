package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.uploaddocuments.DocumentType.REQUEST_FOR_HEARING_RECORDING;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.getOriginalSender;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.isValidAudioVideoDocumentType;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.setHasUnprocessedAudioVideoEvidenceFlag;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.util.AddedDocumentsUtil;
import uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil;
import uk.gov.hmcts.reform.sscs.util.DocumentUtil;

@Component
public class UploadDocumentFurtherEvidenceAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private static final String UPLOAD_DATE_FORMATTER = "yyyy-MM-dd";

    private final AddedDocumentsUtil addedDocumentsUtil;

    private static final Enum<EventType> EVENT_TYPE = EventType.UPLOAD_DOCUMENT_FURTHER_EVIDENCE;

    @Autowired
    public UploadDocumentFurtherEvidenceAboutToSubmitHandler(AddedDocumentsUtil addedDocumentsUtil) {
        this.addedDocumentsUtil = addedDocumentsUtil;
    }


    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        return callbackType != null && callback != null
            && callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
            && callback.getEvent().equals(EVENT_TYPE);
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {
        if (!canHandle(callbackType, callback)) {
            throw new IllegalStateException("Cannot handle callback");
        }

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        addedDocumentsUtil.clearAddedDocumentsBeforeEventSubmit(caseData);
        moveDraftsToSscsDocs(caseData);
        moveDraftsToAudioVideoEvidence(caseData);
        caseData.setEvidenceHandled("No");

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);

        if (nonNull(caseData.getDraftSscsFurtherEvidenceDocument()) && incorrectTypeSelectedForAudioVideoEvidence(caseData.getDraftSscsFurtherEvidenceDocument())) {
            response.addError("Type not accepted for AV evidence. Select a Type for the party that originally submitted the audio/video evidence");
        }

        uploadHearingRecordingRequest(caseData, response);

        initDraftSscsFurtherEvidenceDocument(caseData);
        setHasUnprocessedAudioVideoEvidenceFlag(caseData);
        clearTransientFields(caseData);

        return response;
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

    private boolean incorrectTypeSelectedForAudioVideoEvidence(List<SscsFurtherEvidenceDoc> draftSscsFurtherEvidenceDocuments) {
        return draftSscsFurtherEvidenceDocuments.stream().anyMatch(doc -> DocumentUtil.isFileAMedia(doc.getValue().getDocumentLink()) && !isValidAudioVideoDocumentType(doc.getValue().getDocumentType()));
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
                        .originalPartySender(getOriginalSender(doc.getValue().getDocumentType()))
                        .documentType(AudioVideoEvidenceUtil.getDocumentTypeValue(doc.getValue().getDocumentLink()
                            .getDocumentFilename()))
                        .build())
                    .build())
            .toList();

        addedDocumentsUtil.computeDocumentsAddedThisEvent(sscsCaseData, newAudioVideoEvidence.stream()
            .map(audioVideoEvidence -> audioVideoEvidence.getValue().getDocumentType())
            .filter(Objects::nonNull)
            .toList(), EVENT_TYPE);

        if (!newAudioVideoEvidence.isEmpty()) {
            if (sscsCaseData.getAudioVideoEvidence() == null) {
                sscsCaseData.setAudioVideoEvidence(new ArrayList<>());
            }
            sscsCaseData.getAudioVideoEvidence().addAll(newAudioVideoEvidence);
            if (!REVIEW_BY_JUDGE.equals(sscsCaseData.getInterlocReviewState())) {
                sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_TCW);
            }
            sscsCaseData.setInterlocReferralReason(REVIEW_AUDIO_VIDEO_EVIDENCE);
        }
    }

    private void uploadHearingRecordingRequest(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> response) {

        if (sscsCaseData.getDraftSscsFurtherEvidenceDocument() != null) {
            List<SscsFurtherEvidenceDoc> sscsFurtherEvidenceDocList = sscsCaseData.getDraftSscsFurtherEvidenceDocument().stream()
                .filter(draftDoc -> REQUEST_FOR_HEARING_RECORDING.getId().equals(draftDoc.getValue().getDocumentType())).toList();

            if (sscsCaseData.getSscsHearingRecordingCaseData() != null
                && sscsCaseData.getSscsHearingRecordingCaseData().getRequestingParty() != null
                && sscsCaseData.getSscsHearingRecordingCaseData().getRequestingParty().getValue() != null
                && sscsCaseData.getSscsHearingRecordingCaseData().getRequestingParty().getValue().getCode() != null
                && sscsCaseData.getSscsHearingRecordingCaseData().getSscsHearingRecordings() != null
                && sscsFurtherEvidenceDocList != null && sscsFurtherEvidenceDocList.size() >= 1) {

                DynamicListItem selectedRequestable = sscsCaseData.getSscsHearingRecordingCaseData().getRequestableHearingDetails().getValue();
                String hearingId = selectedRequestable.getCode();

                Optional<SscsHearingRecording> sscsHearingRecording = sscsCaseData.getSscsHearingRecordingCaseData().getSscsHearingRecordings()
                        .stream().filter(r -> r.getValue().getHearingId().equals(hearingId)).findAny();

                if (sscsHearingRecording.isPresent()) {
                    HearingRecordingRequest hearingRecordingRequest = HearingRecordingRequest.builder().value(HearingRecordingRequestDetails.builder()
                            .requestingParty(sscsCaseData.getSscsHearingRecordingCaseData().getRequestingParty().getValue().getCode())
                            .requestDocument(sscsFurtherEvidenceDocList.get(0).getValue().getDocumentLink())
                            .sscsHearingRecording(sscsHearingRecording.get().getValue())
                            .dateRequested(LocalDateTime.now().format(DateTimeFormatter.ofPattern(UPLOAD_DATE_FORMATTER))).build()).build();

                    List<HearingRecordingRequest> hearingRecordingRequests = sscsCaseData.getSscsHearingRecordingCaseData().getRequestedHearings();
                    if (hearingRecordingRequests == null) {
                        hearingRecordingRequests = new ArrayList<>();
                    }
                    hearingRecordingRequests.add(hearingRecordingRequest);

                    sscsCaseData.getSscsHearingRecordingCaseData().setRequestedHearings(hearingRecordingRequests);
                    sscsCaseData.getSscsHearingRecordingCaseData().setHearingRecordingRequestOutstanding(YesNo.YES);
                } else {
                    response.addError("Hearing record not found");
                }
            }
        }
    }

    private void clearTransientFields(SscsCaseData sscsCaseData) {
        sscsCaseData.getSscsHearingRecordingCaseData().setRequestingParty(null);
    }
}
