package uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.DIRECTION_ACTION_REQUIRED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessedAction.DIRECTION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessedAction.SENT_TO_ADMIN;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessedAction.SENT_TO_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.AWAITING_ADMIN_ACTION;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.AWAITING_INFORMATION;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.WELSH_TRANSLATION;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems.*;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.getDocumentType;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.isSelectedEvidence;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@Service
public class ProcessAudioVideoEvidenceAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    public static final List<String> ACTIONS_THAT_REQUIRES_NOTICE = asList(ISSUE_DIRECTIONS_NOTICE.getCode(), EXCLUDE_EVIDENCE.getCode(), INCLUDE_EVIDENCE.getCode());

    private final FooterService footerService;

    @Autowired
    public ProcessAudioVideoEvidenceAboutToSubmitHandler(FooterService footerService) {
        this.footerService = footerService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.PROCESS_AUDIO_VIDEO;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback, String userAuthorisation) {

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData caseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(caseData);
        if (isNull(caseData.getProcessAudioVideoAction()) || isNull(caseData.getProcessAudioVideoAction().getValue())) {
            response.addError("Select an action to process the audio/video evidence");
            return response;
        }

        if (ACTIONS_THAT_REQUIRES_NOTICE.contains(caseData.getProcessAudioVideoAction().getValue().getCode())) {
            if (isNull(caseData.getPreviewDocument())) {
                response.addError("There is no document notice");
            } else {
                addDirectionNotice(caseData);
            }
        }
        if (!isEmpty(response.getErrors())) {
            return response;
        }
        processIfIssueDirectionNotice(caseData);
        processIfIncludeEvidence(caseData, response);
        processIfExcludeEvidence(caseData);
        processIfSendToJudge(caseData);
        processIfSendToAdmin(caseData);

        clearTransientFields(caseData);

        return response;
    }

    private void addDirectionNotice(SscsCaseData caseData) {
        DocumentLink url = caseData.getPreviewDocument();
        SscsDocumentTranslationStatus documentTranslationStatus = caseData.isLanguagePreferenceWelsh() ? SscsDocumentTranslationStatus.TRANSLATION_REQUIRED : null;
        footerService.createFooterAndAddDocToCase(url, caseData, AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE,
                Optional.ofNullable(caseData.getDateAdded()).orElse(LocalDate.now())
                        .format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                caseData.getDateAdded(), null, documentTranslationStatus);
    }

    private void processIfIssueDirectionNotice(SscsCaseData caseData) {
        if (StringUtils.equals(caseData.getProcessAudioVideoAction().getValue().getCode(), ISSUE_DIRECTIONS_NOTICE.getCode())) {
            caseData.setInterlocReviewState(AWAITING_INFORMATION.getId());
            caseData.setDwpState(DwpState.DIRECTION_ACTION_REQUIRED.getId());
            if (caseData.isLanguagePreferenceWelsh()) {
                caseData.setWelshInterlocNextReviewState(AWAITING_INFORMATION.getId());
                caseData.setInterlocReviewState(WELSH_TRANSLATION.getId());
            } else {
                caseData.setInterlocReviewState(AWAITING_INFORMATION.getId());
            }
            caseData.setInterlocReferralDate(LocalDate.now().toString());
            addProcessedActionToSelectedEvidence(caseData, DIRECTION_ISSUED);
        }
    }

    private void processIfIncludeEvidence(SscsCaseData caseData, PreSubmitCallbackResponse<SscsCaseData> response) {
        if (StringUtils.equals(caseData.getProcessAudioVideoAction().getValue().getCode(), INCLUDE_EVIDENCE.getCode())) {
            caseData.setInterlocReviewState(null);
            caseData.setDwpState(DIRECTION_ACTION_REQUIRED.getId());

            List<SscsDocument> sscsDocuments = new ArrayList<>();
            List<DwpDocument> dwpDocuments = new ArrayList<>();

            AudioVideoEvidenceDetails selectedAudioVideoEvidenceDetails = caseData.getSelectedAudioVideoEvidenceDetails();

            if (UploadParty.DWP.equals(selectedAudioVideoEvidenceDetails.getPartyUploaded())) {
                dwpDocuments.add(buildAudioVideoDwpDocument(selectedAudioVideoEvidenceDetails, response, caseData.isLanguagePreferenceWelsh()));
            } else {
                sscsDocuments.add(buildAudioVideoSscsDocument(selectedAudioVideoEvidenceDetails, response));
            }

            if (caseData.getDwpDocuments() != null) {
                dwpDocuments.addAll(caseData.getDwpDocuments());
            }

            caseData.setDwpDocuments(dwpDocuments);

            if (caseData.isLanguagePreferenceWelsh() && isAnyRip1Doc(dwpDocuments)) {
                caseData.setInterlocReviewState(WELSH_TRANSLATION.getId());
            }

            if (caseData.getSscsDocument() != null) {
                sscsDocuments.addAll(caseData.getSscsDocument());
            }

            caseData.setSscsDocument(sscsDocuments);

            caseData.getAudioVideoEvidence().removeIf(evidence -> isSelectedEvidence(evidence, caseData));
        }
    }

    private boolean isAnyRip1Doc(List<DwpDocument> dwpDocuments) {
        return dwpDocuments.stream().anyMatch(d -> d.getValue().getRip1DocumentLink() != null);
    }

    private DwpDocument buildAudioVideoDwpDocument(AudioVideoEvidenceDetails audioVideoEvidence, PreSubmitCallbackResponse<SscsCaseData> response, boolean isWelshCase) {
        DocumentLink rip1Doc = null;
        SscsDocumentTranslationStatus status = null;
        if (audioVideoEvidence.getRip1Document() != null) {
            rip1Doc = buildRip1Doc(audioVideoEvidence);
            if (isWelshCase) {
                status = SscsDocumentTranslationStatus.TRANSLATION_REQUIRED;
            }
        }

        return DwpDocument.builder().value(
                DwpDocumentDetails.builder()
                        .documentLink(audioVideoEvidence.getDocumentLink())
                        .documentFileName(audioVideoEvidence.getFileName())
                        .documentType(findAudioVideoDocumentType(audioVideoEvidence, response))
                        .documentDateAdded(audioVideoEvidence.getDateAdded().toString())
                        .partyUploaded(audioVideoEvidence.getPartyUploaded())
                        .dateApproved(LocalDate.now().toString())
                        .rip1DocumentLink(rip1Doc)
                        .documentTranslationStatus(status)
                        .build())
                .build();
    }

    private DocumentLink buildRip1Doc(AudioVideoEvidenceDetails audioVideoEvidence) {
        String rip1FileName = "RIP 1 document uploaded on " + audioVideoEvidence.getDateAdded().toString() + ".pdf";

        return DocumentLink.builder()
                .documentFilename(rip1FileName)
                .documentUrl(audioVideoEvidence.getRip1Document().getDocumentUrl())
                .documentBinaryUrl(audioVideoEvidence.getRip1Document().getDocumentBinaryUrl())
                .build();

    }

    private SscsDocument buildAudioVideoSscsDocument(AudioVideoEvidenceDetails audioVideoEvidence, PreSubmitCallbackResponse<SscsCaseData> response) {
        return SscsDocument.builder().value(
                SscsDocumentDetails.builder()
                        .documentLink(audioVideoEvidence.getDocumentLink())
                        .documentFileName(audioVideoEvidence.getFileName())
                        .documentType(findAudioVideoDocumentType(audioVideoEvidence, response))
                        .documentDateAdded(audioVideoEvidence.getDateAdded().toString())
                        .partyUploaded(audioVideoEvidence.getPartyUploaded())
                        .dateApproved(LocalDate.now().toString())
                        .build())
                .build();
    }

    private String findAudioVideoDocumentType(AudioVideoEvidenceDetails audioVideoEvidence, PreSubmitCallbackResponse<SscsCaseData> response) {
        DocumentType documentType = getDocumentType(audioVideoEvidence);

        if (isNull(documentType)) {
            response.addError("Evidence cannot be included as it is not in .mp3 or .mp4 format");
            return null;
        } else {
            return documentType.getValue();
        }
    }

    private void addToNotesIfNoteExists(SscsCaseData caseData) {
        if (StringUtils.isNoneBlank(caseData.getAppealNote())) {
            ArrayList<Note> notes = new ArrayList<>(Optional.ofNullable(caseData.getAppealNotePad()).flatMap(f -> Optional.ofNullable(f.getNotesCollection())).orElse(Collections.emptyList()));
            final NoteDetails noteDetail = NoteDetails.builder().noteDetail(caseData.getAppealNote()).noteDate(LocalDate.now().toString()).build();
            notes.add(Note.builder().value(noteDetail).build());
            caseData.setAppealNotePad(NotePad.builder().notesCollection(notes).build());
        }
    }

    private void processIfExcludeEvidence(SscsCaseData caseData) {
        if (StringUtils.equals(caseData.getProcessAudioVideoAction().getValue().getCode(), EXCLUDE_EVIDENCE.getCode())) {
            caseData.setInterlocReviewState(null);
            caseData.setDwpState(DIRECTION_ACTION_REQUIRED.getId());
            caseData.getAudioVideoEvidence().removeIf(evidence -> isSelectedEvidence(evidence, caseData));
        }
    }

    private void processIfSendToJudge(SscsCaseData caseData) {
        if (StringUtils.equals(caseData.getProcessAudioVideoAction().getValue().getCode(), SEND_TO_JUDGE.getCode())) {
            if (caseData.isLanguagePreferenceWelsh()) {
                caseData.setWelshInterlocNextReviewState(REVIEW_BY_JUDGE.getId());
                caseData.setInterlocReviewState(WELSH_TRANSLATION.getId());
            } else {
                caseData.setInterlocReviewState(REVIEW_BY_JUDGE.getId());
            }
            caseData.setInterlocReferralDate(LocalDate.now().toString());
            addToNotesIfNoteExists(caseData);
            addProcessedActionToSelectedEvidence(caseData, SENT_TO_JUDGE);
        }
    }

    private void processIfSendToAdmin(SscsCaseData caseData) {
        if (StringUtils.equals(caseData.getProcessAudioVideoAction().getValue().getCode(), SEND_TO_ADMIN.getCode())) {
            caseData.setInterlocReviewState(AWAITING_ADMIN_ACTION.getId());
            if (caseData.isLanguagePreferenceWelsh()) {
                caseData.setWelshInterlocNextReviewState(AWAITING_ADMIN_ACTION.getId());
                caseData.setInterlocReviewState(WELSH_TRANSLATION.getId());
            } else {
                caseData.setInterlocReviewState(AWAITING_ADMIN_ACTION.getId());
            }
            addToNotesIfNoteExists(caseData);
            addProcessedActionToSelectedEvidence(caseData, SENT_TO_ADMIN);
        }
    }

    private void addProcessedActionToSelectedEvidence(SscsCaseData caseData, ProcessedAction processedAction) {
        caseData.getAudioVideoEvidence().stream().filter(evidence -> isSelectedEvidence(evidence, caseData))
                .forEach(evidence -> evidence.getValue().setProcessedAction(processedAction));
    }

    private void clearTransientFields(SscsCaseData caseData) {
        caseData.setBodyContent(null);
        caseData.setPreviewDocument(null);
        caseData.setGenerateNotice(null);
        caseData.setReservedToJudge(null);
        caseData.setGenerateNotice(null);
        caseData.setSignedBy(null);
        caseData.setSignedRole(null);
        caseData.setDateAdded(null);
        caseData.setAppealNote(null);
        caseData.setSelectedAudioVideoEvidenceDetails(null);
        caseData.setShowRip1DocPage(null);
    }
}
