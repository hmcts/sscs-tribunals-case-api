package uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo;

import static java.util.Arrays.asList;
import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.AWAITING_INFORMATION;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.processaudiovideo.ProcessAudioVideoActionDynamicListItems.*;

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
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@Service
public class ProcessAudioVideoEvidenceAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    public static final List<String> ACTIONS_THAT_REQUIRES_NOTICE = asList(ISSUE_DIRECTIONS_NOTICE.getCode(), EXCLUDE_EVIDENCE.getCode());

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
        processIfExcludeEvidence(caseData);
        processIfSendToJudge(caseData);
        processIfSendToAdmin(caseData);

        clearTransientFields(caseData);

        return response;
    }

    private void addDirectionNotice(SscsCaseData caseData) {
        DocumentLink url = caseData.getPreviewDocument();
        footerService.createFooterAndAddDocToCase(url, caseData, DocumentType.DIRECTION_NOTICE,
                Optional.ofNullable(caseData.getDateAdded()).orElse(LocalDate.now())
                        .format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                caseData.getDateAdded(), null, null);
    }

    private void processIfIssueDirectionNotice(SscsCaseData caseData) {
        if (StringUtils.equals(caseData.getProcessAudioVideoAction().getValue().getCode(), ISSUE_DIRECTIONS_NOTICE.getCode())) {
            caseData.setInterlocReviewState(AWAITING_INFORMATION.getId());
            caseData.setDwpState(DwpState.DIRECTION_ACTION_REQUIRED.getId());
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
            caseData.setDwpState(DwpState.DIRECTION_ACTION_REQUIRED.getId());
            caseData.setAudioVideoEvidence(null);
        }
    }

    private void processIfSendToJudge(SscsCaseData caseData) {
        if (StringUtils.equals(caseData.getProcessAudioVideoAction().getValue().getCode(), SEND_TO_JUDGE.getCode())) {
            if (caseData.isLanguagePreferenceWelsh()) {
                caseData.setWelshInterlocNextReviewState(InterlocReviewState.REVIEW_BY_JUDGE.getId());
                caseData.setInterlocReviewState(InterlocReviewState.WELSH_TRANSLATION.getId());
            } else {
                caseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE.getId());
            }
            caseData.setInterlocReferralDate(LocalDate.now().toString());
            addToNotesIfNoteExists(caseData);
        }
    }

    private void processIfSendToAdmin(SscsCaseData caseData) {
        if (StringUtils.equals(caseData.getProcessAudioVideoAction().getValue().getCode(), SEND_TO_ADMIN.getCode())) {
            caseData.setInterlocReviewState(InterlocReviewState.AWAITING_ADMIN_ACTION.getId());
            if (caseData.isLanguagePreferenceWelsh()) {
                caseData.setWelshInterlocNextReviewState(InterlocReviewState.AWAITING_ADMIN_ACTION.getId());
                caseData.setInterlocReviewState(InterlocReviewState.WELSH_TRANSLATION.getId());
            } else {
                caseData.setInterlocReviewState(InterlocReviewState.AWAITING_ADMIN_ACTION.getId());
            }
            addToNotesIfNoteExists(caseData);
        }
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
    }
}
