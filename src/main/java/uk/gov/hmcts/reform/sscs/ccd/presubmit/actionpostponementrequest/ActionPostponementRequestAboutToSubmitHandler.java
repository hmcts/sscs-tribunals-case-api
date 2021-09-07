package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionpostponementrequest;

import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction.GRANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction.SEND_TO_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ListingOption.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ListingOption.NOT_LISTABLE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;


@Service
@Slf4j
public class ActionPostponementRequestAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    private UserDetailsService userDetailsService;
    private final FooterService footerService;

    @Autowired
    public ActionPostponementRequestAboutToSubmitHandler(UserDetailsService userDetailsService, FooterService footerService) {
        this.userDetailsService = userDetailsService;
        this.footerService = footerService;
    }

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.ACTION_POSTPONEMENT_REQUEST;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        PostponementRequest postponementRequest = sscsCaseData.getPostponementRequest();

        if (isSendToJudge(postponementRequest)) {
            sendToJudge(userAuthorisation, sscsCaseData);
        } else if (isGrantPostponement(postponementRequest)) {
            if (isReadyToList(postponementRequest)) {
                sscsCaseData.setInterlocReferralReason(null);
                sscsCaseData.setInterlocReviewState(null);
            }
            addDirectionNotice(sscsCaseData);
            sscsCaseData.setPostponementRequest(PostponementRequest.builder().unprocessedPostponementRequest(YesNo.NO)
                    .build());
        }

        return response;
    }

    private boolean isReadyToList(PostponementRequest postponementRequest) {
        return postponementRequest.getListingOption().equals(READY_TO_LIST.getValue());
    }

    private boolean isGrantPostponement(PostponementRequest postponementRequest) {
        return postponementRequest.getActionPostponementRequestSelected().equals(GRANT.getValue());
    }

    private boolean isSendToJudge(PostponementRequest postponementRequest) {
        return postponementRequest.getActionPostponementRequestSelected().equals(SEND_TO_JUDGE.getValue());
    }

    private void sendToJudge(String userAuthorisation, SscsCaseData sscsCaseData) {
        if (sscsCaseData.getAppealNotePad() == null) {
            sscsCaseData.setAppealNotePad(NotePad.builder().notesCollection(new ArrayList<>()).build());
        }
        sscsCaseData.getAppealNotePad().getNotesCollection()
                .add(createPostponementRequestNote(userAuthorisation,
                        sscsCaseData.getPostponementRequest().getPostponementRequestDetails()));
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE.getId());
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST.getId());
        sscsCaseData.setPostponementRequest(PostponementRequest.builder().unprocessedPostponementRequest(YesNo.YES)
                .build());
    }

    private Note createPostponementRequestNote(String userAuthorisation, String details) {
        return Note.builder().value(NoteDetails.builder().noteDetail(details)
                .author(userDetailsService.buildLoggedInUserName(userAuthorisation))
                .noteDate(LocalDate.now().toString()).build()).build();
    }

    private void addDirectionNotice(SscsCaseData caseData) {
        DocumentLink url = caseData.getPreviewDocument();
        SscsDocumentTranslationStatus documentTranslationStatus = caseData.isLanguagePreferenceWelsh() ? SscsDocumentTranslationStatus.TRANSLATION_REQUIRED : null;
        footerService.createFooterAndAddDocToCase(url, caseData, AUDIO_VIDEO_EVIDENCE_DIRECTION_NOTICE,
                Optional.ofNullable(caseData.getDateAdded()).orElse(LocalDate.now())
                        .format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                caseData.getDateAdded(), null, documentTranslationStatus);
    }
}
