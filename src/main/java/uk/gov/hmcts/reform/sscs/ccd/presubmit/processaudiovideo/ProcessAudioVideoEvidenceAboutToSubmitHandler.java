package uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.RIP1;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.DIRECTION_ACTION_REQUIRED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.AWAITING_ADMIN_ACTION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.AWAITING_INFORMATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.REVIEW_BY_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.WELSH_TRANSLATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessedAction.DIRECTION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessedAction.SENT_TO_ADMIN;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessedAction.SENT_TO_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems.ADMIT_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems.EXCLUDE_EVIDENCE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems.ISSUE_DIRECTIONS_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems.SEND_TO_ADMIN;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems.SEND_TO_JUDGE;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.getDocumentType;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.isSelectedEvidence;
import static uk.gov.hmcts.reform.sscs.util.AudioVideoEvidenceUtil.setHasUnprocessedAudioVideoEvidenceFlag;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.AudioVideoEvidenceDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentGeneration;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentStaging;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.domain.Note;
import uk.gov.hmcts.reform.sscs.ccd.domain.NoteDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.NotePad;
import uk.gov.hmcts.reform.sscs.ccd.domain.ProcessedAction;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.UploadParty;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;

@Service
@Slf4j
@AllArgsConstructor
public class ProcessAudioVideoEvidenceAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    public static final List<String> ACTIONS_THAT_REQUIRES_NOTICE = asList(ISSUE_DIRECTIONS_NOTICE.getCode(), EXCLUDE_EVIDENCE.getCode(), ADMIT_EVIDENCE.getCode());

    private final FooterService footerService;
    protected final UserDetailsService userDetailsService;

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
            if (isNull(caseData.getDocumentStaging().getPreviewDocument())) {
                response.addError("There is no document notice");
            } else {
                addDirectionNotice(caseData);
            }
        }
        if (!isEmpty(response.getErrors())) {
            return response;
        }
        processIfIssueDirectionNotice(caseData);
        processIfAdmitEvidence(caseData, response);
        processIfExcludeEvidence(caseData);
        processIfSendToJudge(caseData, userAuthorisation);
        processIfSendToAdmin(caseData, userAuthorisation);
        overrideInterlocReviewStateIfSelected(caseData);

        clearEmptyAudioVideoList(caseData);
        clearTransientFields(caseData);
        caseData.updateTranslationWorkOutstandingFlag();
        setHasUnprocessedAudioVideoEvidenceFlag(caseData);

        return response;
    }

    private void addDirectionNotice(SscsCaseData caseData) {
        DocumentLink url = caseData.getDocumentStaging().getPreviewDocument();
        SscsDocumentTranslationStatus documentTranslationStatus = caseData.isLanguagePreferenceWelsh() ? SscsDocumentTranslationStatus.TRANSLATION_REQUIRED : null;
        footerService.createFooterAndAddDocToCase(url, caseData, AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE,
                Optional.ofNullable(caseData.getDocumentStaging().getDateAdded()).orElse(LocalDate.now())
                        .format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                caseData.getDocumentStaging().getDateAdded(), null, documentTranslationStatus);
    }

    private void processIfIssueDirectionNotice(SscsCaseData caseData) {
        if (StringUtils.equals(caseData.getProcessAudioVideoAction().getValue().getCode(), ISSUE_DIRECTIONS_NOTICE.getCode())) {
            caseData.setInterlocReviewState(AWAITING_INFORMATION);
            caseData.setDwpState(DwpState.DIRECTION_ACTION_REQUIRED);
            if (caseData.isLanguagePreferenceWelsh()) {
                caseData.setWelshInterlocNextReviewState(AWAITING_INFORMATION.getCcdDefinition());
                caseData.setInterlocReviewState(WELSH_TRANSLATION);
            } else {
                caseData.setInterlocReviewState(AWAITING_INFORMATION);
            }
            caseData.setInterlocReferralReason(InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE);
            caseData.setInterlocReferralDate(LocalDate.now());
            addProcessedActionToSelectedEvidence(caseData, DIRECTION_ISSUED);
        }
    }

    private void processIfAdmitEvidence(SscsCaseData caseData, PreSubmitCallbackResponse<SscsCaseData> response) {
        if (StringUtils.equals(caseData.getProcessAudioVideoAction().getValue().getCode(), ADMIT_EVIDENCE.getCode())) {
            caseData.setInterlocReviewState(null);
            caseData.setInterlocReferralReason(InterlocReferralReason.NONE);
            caseData.setDwpState(DIRECTION_ACTION_REQUIRED);

            List<SscsDocument> sscsDocuments = new ArrayList<>();
            List<DwpDocument> dwpDocuments = new ArrayList<>();

            AudioVideoEvidenceDetails selectedAudioVideoEvidenceDetails = caseData.getSelectedAudioVideoEvidenceDetails();

            if (UploadParty.DWP.equals(selectedAudioVideoEvidenceDetails.getPartyUploaded())) {
                dwpDocuments.add(buildAudioVideoDwpDocument(selectedAudioVideoEvidenceDetails, response));
                if (selectedAudioVideoEvidenceDetails.getRip1Document() != null) {
                    sscsDocuments.add(buildRip1Document(selectedAudioVideoEvidenceDetails, response, caseData.isLanguagePreferenceWelsh()));
                }
            } else {
                sscsDocuments.add(buildAudioVideoSscsDocument(selectedAudioVideoEvidenceDetails, response));
            }

            if (caseData.getDwpDocuments() != null) {
                dwpDocuments.addAll(caseData.getDwpDocuments());
            }

            caseData.setDwpDocuments(dwpDocuments);

            if (caseData.getSscsDocument() != null) {
                sscsDocuments.addAll(caseData.getSscsDocument());
            }

            caseData.setSscsDocument(sscsDocuments);

            caseData.getAudioVideoEvidence().removeIf(evidence -> isSelectedEvidence(evidence, caseData));
        }
    }

    private DwpDocument buildAudioVideoDwpDocument(AudioVideoEvidenceDetails audioVideoEvidence, PreSubmitCallbackResponse<SscsCaseData> response) {

        return DwpDocument.builder().value(
                DwpDocumentDetails.builder()
                        .avDocumentLink(audioVideoEvidence.getDocumentLink())
                        .documentFileName(audioVideoEvidence.getFileName())
                        .documentType(findAudioVideoDocumentType(audioVideoEvidence, response))
                        .documentDateAdded(audioVideoEvidence.getDateAdded().toString())
                        .partyUploaded(audioVideoEvidence.getPartyUploaded())
                        .dateApproved(LocalDate.now().toString())
                        .build())
                .build();
    }

    private SscsDocument buildRip1Document(AudioVideoEvidenceDetails audioVideoEvidence, PreSubmitCallbackResponse<SscsCaseData> response, boolean isWelshCase) {

        DocumentLink rip1Doc = buildRip1DocumentLink(audioVideoEvidence);
        String bundleAddition = footerService.getNextBundleAddition(response.getData().getSscsDocument());
        DocumentLink url = footerService.addFooter(rip1Doc, RIP1.getLabel(), bundleAddition);
        String fileName = "Addition " + bundleAddition + " - " + audioVideoEvidence.getPartyUploaded().getLabel() + " - RIP 1 document for A/V file: " + audioVideoEvidence.getFileName();

        SscsDocumentTranslationStatus status = null;

        if (isWelshCase) {
            response.getData().setInterlocReviewState(WELSH_TRANSLATION);
            status = SscsDocumentTranslationStatus.TRANSLATION_REQUIRED;
        }

        return SscsDocument.builder().value(
                SscsDocumentDetails.builder()
                        .documentFileName(fileName)
                        .documentType(RIP1.getValue())
                        .documentDateAdded(audioVideoEvidence.getDateAdded().toString())
                        .partyUploaded(audioVideoEvidence.getPartyUploaded())
                        .originalSenderOtherPartyId(audioVideoEvidence.getOriginalSenderOtherPartyId())
                        .originalSenderOtherPartyName(audioVideoEvidence.getOriginalSenderOtherPartyName())
                        .dateApproved(LocalDate.now().toString())
                        .documentTranslationStatus(status)
                        .documentLink(url)
                        .bundleAddition(bundleAddition)
                        .build())
                .build();
    }

    private DocumentLink buildRip1DocumentLink(AudioVideoEvidenceDetails audioVideoEvidence) {
        String rip1FileName = "RIP 1 document uploaded on " + audioVideoEvidence.getDateAdded().toString() + ".pdf";

        return DocumentLink.builder()
                .documentFilename(rip1FileName)
                .documentUrl(audioVideoEvidence.getRip1Document().getDocumentUrl())
                .documentBinaryUrl(audioVideoEvidence.getRip1Document().getDocumentBinaryUrl())
                .build();

    }

    private SscsDocument buildAudioVideoSscsDocument(AudioVideoEvidenceDetails audioVideoEvidence, PreSubmitCallbackResponse<SscsCaseData> response) {
        String fileName = audioVideoEvidence.getFileName();
        String bundleAddition = null;
        DocumentLink url = null;

        if (audioVideoEvidence.getStatementOfEvidencePdf() != null) {
            bundleAddition = footerService.getNextBundleAddition(response.getData().getSscsDocument());
            url = footerService.addFooter(audioVideoEvidence.getStatementOfEvidencePdf(), "Statement of audio/video evidence", bundleAddition);
            fileName = "Addition " + bundleAddition + " - " + audioVideoEvidence.getPartyUploaded().getLabel() + " - Statement for A/V file: " + fileName;
        }

        return SscsDocument.builder().value(
                SscsDocumentDetails.builder()
                        .avDocumentLink(audioVideoEvidence.getDocumentLink())
                        .documentFileName(fileName)
                        .documentType(findAudioVideoDocumentType(audioVideoEvidence, response))
                        .documentDateAdded(audioVideoEvidence.getDateAdded().toString())
                        .partyUploaded(audioVideoEvidence.getPartyUploaded())
                        .originalSenderOtherPartyId(audioVideoEvidence.getOriginalSenderOtherPartyId())
                        .originalSenderOtherPartyName(audioVideoEvidence.getOriginalSenderOtherPartyName())
                        .dateApproved(LocalDate.now().toString())
                        .originalPartySender(audioVideoEvidence.getOriginalPartySender())
                        .documentLink(url)
                        .bundleAddition(bundleAddition)
                        .build())
                .build();
    }

    private String findAudioVideoDocumentType(AudioVideoEvidenceDetails audioVideoEvidence, PreSubmitCallbackResponse<SscsCaseData> response) {
        DocumentType documentType = getDocumentType(audioVideoEvidence.getDocumentLink().getDocumentFilename());

        if (isNull(documentType)) {
            response.addError("Evidence cannot be included as it is not in .mp3 or .mp4 format");
            return null;
        } else {
            return documentType.getValue();
        }
    }

    private void addToNotesIfNoteExists(SscsCaseData caseData, String userAuthorisation) {
        if (StringUtils.isNoneBlank(caseData.getTempNoteDetail())) {
            ArrayList<Note> notes = new ArrayList<>(Optional.ofNullable(caseData.getAppealNotePad()).flatMap(f -> Optional.ofNullable(f.getNotesCollection())).orElse(Collections.emptyList()));
            final NoteDetails noteDetail = NoteDetails.builder().noteDetail(caseData.getTempNoteDetail()).noteDate(LocalDate.now().toString()).author(userDetailsService.buildLoggedInUserName(userAuthorisation)).build();
            notes.add(Note.builder().value(noteDetail).build());
            caseData.setAppealNotePad(NotePad.builder().notesCollection(notes).build());
        }
    }

    private void processIfExcludeEvidence(SscsCaseData caseData) {
        if (StringUtils.equals(caseData.getProcessAudioVideoAction().getValue().getCode(), EXCLUDE_EVIDENCE.getCode())) {
            caseData.setInterlocReviewState(null);
            caseData.setInterlocReferralReason(InterlocReferralReason.NONE);
            caseData.setDwpState(DIRECTION_ACTION_REQUIRED);
            caseData.getAudioVideoEvidence().removeIf(evidence -> isSelectedEvidence(evidence, caseData));
        }
    }

    private void processIfSendToJudge(SscsCaseData caseData, String userAuthorisation) {
        if (StringUtils.equals(caseData.getProcessAudioVideoAction().getValue().getCode(), SEND_TO_JUDGE.getCode())) {
            if (caseData.isLanguagePreferenceWelsh()) {
                caseData.setWelshInterlocNextReviewState(REVIEW_BY_JUDGE.getCcdDefinition());
                caseData.setInterlocReviewState(WELSH_TRANSLATION);
            } else {
                caseData.setInterlocReviewState(REVIEW_BY_JUDGE);
            }
            caseData.setInterlocReferralReason(InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE);
            caseData.setInterlocReferralDate(LocalDate.now());
            addToNotesIfNoteExists(caseData, userAuthorisation);
            addProcessedActionToSelectedEvidence(caseData, SENT_TO_JUDGE);
        }
    }

    private void processIfSendToAdmin(SscsCaseData caseData, String userAuthorisation) {
        if (StringUtils.equals(caseData.getProcessAudioVideoAction().getValue().getCode(), SEND_TO_ADMIN.getCode())) {
            if (caseData.isLanguagePreferenceWelsh()) {
                caseData.setWelshInterlocNextReviewState(AWAITING_ADMIN_ACTION.getCcdDefinition());
                caseData.setInterlocReviewState(WELSH_TRANSLATION);
            } else {
                caseData.setInterlocReviewState(AWAITING_ADMIN_ACTION);
            }
            caseData.setInterlocReferralReason(InterlocReferralReason.REVIEW_AUDIO_VIDEO_EVIDENCE);
            addToNotesIfNoteExists(caseData, userAuthorisation);
            addProcessedActionToSelectedEvidence(caseData, SENT_TO_ADMIN);
        }
    }

    private void addProcessedActionToSelectedEvidence(SscsCaseData caseData, ProcessedAction processedAction) {
        caseData.getAudioVideoEvidence().stream().filter(evidence -> isSelectedEvidence(evidence, caseData))
                .forEach(evidence -> evidence.getValue().setProcessedAction(processedAction));
    }

    private void overrideInterlocReviewStateIfSelected(SscsCaseData caseData) {
        if (caseData.getProcessAudioVideoReviewState() != null) {
            switch (caseData.getProcessAudioVideoReviewState()) {
                case AWAITING_INFORMATION:
                    caseData.setInterlocReviewState(AWAITING_INFORMATION);
                    break;
                case REVIEW_BY_JUDGE:
                    caseData.setInterlocReviewState(REVIEW_BY_JUDGE);
                    break;
                case AWAITING_ADMIN_ACTION:
                    caseData.setInterlocReviewState(AWAITING_ADMIN_ACTION);
                    break;
                case CLEAR_INTERLOC_REVIEW_STATE:
                    caseData.setInterlocReviewState(null);
                    break;
                default:
                    break;
            }

        }
    }

    private void clearEmptyAudioVideoList(SscsCaseData caseData) {
        if (caseData.getAudioVideoEvidence() != null && caseData.getAudioVideoEvidence().isEmpty()) {
            caseData.setAudioVideoEvidence(null);
        }
    }

    private void clearTransientFields(SscsCaseData caseData) {
        caseData.setDocumentGeneration(DocumentGeneration.builder().build());
        caseData.setDocumentStaging(DocumentStaging.builder().build());
        caseData.setTempNoteDetail(null);
        caseData.setSelectedAudioVideoEvidenceDetails(null);
        caseData.setShowRip1DocPage(null);
        caseData.setProcessAudioVideoReviewState(null);
    }
}
