package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionpostponementrequest;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.POSTPONEMENT_REQUEST_DIRECTION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.NOT_LISTABLE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;


@Service
@Slf4j
public class ActionPostponementRequestAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    public static final String POSTPONEMENT_DETAILS_SENT_TO_JUDGE_PREFIX = "Postponement sent to judge - ";

    private UserDetailsService userDetailsService;
    private final FooterService footerService;
    private final ListAssistHearingMessageHelper hearingMessageHelper;
    private boolean isScheduleListingEnabled;

    public ActionPostponementRequestAboutToSubmitHandler(UserDetailsService userDetailsService,
        FooterService footerService, ListAssistHearingMessageHelper hearingMessageHelper,
            @Value("${feature.snl.enabled}") boolean isScheduleListingEnabled) {
        this.userDetailsService = userDetailsService;
        this.footerService = footerService;
        this.hearingMessageHelper = hearingMessageHelper;
        this.isScheduleListingEnabled = isScheduleListingEnabled;
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
            grantPostponement(sscsCaseData, postponementRequest);
            setHearingDateToExcludedDate(sscsCaseData, response);
            cancelHearing(sscsCaseData);
            if (isScheduleListingEnabled) {
                sscsCaseData.setDwpState(DwpState.HEARING_POSTPONED.getId());
            }
        } else if (isRefusePostponement(postponementRequest)) {
            clearInterlocAndSetFlags(sscsCaseData);
        }

        clearTransientFields(sscsCaseData);
        return response;
    }

    private void cancelHearing(SscsCaseData sscsCaseData) {
        if (eligibleForHearingsCancel.test(sscsCaseData)) {
            log.info("Action postponement request: Sending cancel hearing request for case {}", sscsCaseData
                    .getCcdCaseId());
            hearingMessageHelper.sendListAssistCancelHearingMessage(sscsCaseData.getCcdCaseId(),
                    CancellationReason.OTHER);
        }
    }

    private final Predicate<SscsCaseData> eligibleForHearingsCancel = sscsCaseData -> isScheduleListingEnabled
            && SscsUtil.isSAndLCase(sscsCaseData);

    private void grantPostponement(SscsCaseData sscsCaseData, PostponementRequest postponementRequest) {
        if (isReadyToList(postponementRequest)) {
            sscsCaseData.setState(State.READY_TO_LIST);
        } else if (isNotListable(postponementRequest)) {
            sscsCaseData.setState(State.NOT_LISTABLE);
        }

        clearInterlocAndSetFlags(sscsCaseData);
    }

    private void setHearingDateToExcludedDate(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> response) {
        final Optional<Hearing> optionalHearing = emptyIfNull(sscsCaseData.getHearings()).stream()
                .filter(h -> h.getValue().getHearingDateTime().isAfter(LocalDateTime.now()))
                .distinct()
                .findFirst();

        optionalHearing.ifPresentOrElse(hearing ->
                        footerService.setHearingDateAsExcludeDate(hearing, sscsCaseData),
                () -> response.addError("There are no hearing to postpone"));
    }

    private void clearInterlocAndSetFlags(SscsCaseData sscsCaseData) {

        sscsCaseData.setInterlocReferralReason(null);
        sscsCaseData.setInterlocReviewState(null);
        sscsCaseData.setDwpState(DwpState.DIRECTION_ACTION_REQUIRED.getId());
        addDirectionNotice(sscsCaseData);
        sscsCaseData.setPostponementRequest(PostponementRequest.builder().unprocessedPostponementRequest(YesNo.NO)
                .build());
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

    private void addDirectionNotice(SscsCaseData caseData) {
        DocumentLink url = caseData.getPreviewDocument();
        String now = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        SscsDocumentTranslationStatus documentTranslationStatus =
                caseData.isLanguagePreferenceWelsh() ? SscsDocumentTranslationStatus.TRANSLATION_REQUIRED : null;
        footerService.createFooterAndAddDocToCase(url, caseData, POSTPONEMENT_REQUEST_DIRECTION_NOTICE, now,
                null, null, documentTranslationStatus);
        if (documentTranslationStatus != null) {
            caseData.setInterlocReviewState(InterlocReviewState.WELSH_TRANSLATION.getId());
            log.info("Set the InterlocReviewState to {},  for case id : {}", caseData.getInterlocReviewState(), caseData.getCcdCaseId());
            caseData.setTranslationWorkOutstanding(YES.getValue());
        }
    }

    private Note createPostponementRequestNote(String userAuthorisation, String details) {
        return Note.builder().value(NoteDetails.builder().noteDetail(POSTPONEMENT_DETAILS_SENT_TO_JUDGE_PREFIX + details)
                .author(userDetailsService.buildLoggedInUserName(userAuthorisation))
                .noteDate(LocalDate.now().toString()).build()).build();
    }

    private boolean isNotListable(PostponementRequest postponementRequest) {
        return postponementRequest.getListingOption().equals(NOT_LISTABLE.getId());
    }

    private boolean isReadyToList(PostponementRequest postponementRequest) {
        return postponementRequest.getListingOption().equals(READY_TO_LIST.getId());
    }

    private boolean isGrantPostponement(PostponementRequest postponementRequest) {
        return postponementRequest.getActionPostponementRequestSelected().equals(GRANT.getValue());
    }

    private boolean isSendToJudge(PostponementRequest postponementRequest) {
        return postponementRequest.getActionPostponementRequestSelected().equals(SEND_TO_JUDGE.getValue());
    }

    private boolean isRefusePostponement(PostponementRequest postponementRequest) {
        return postponementRequest.getActionPostponementRequestSelected().equals(REFUSE.getValue());
    }

    private void clearTransientFields(SscsCaseData caseData) {
        caseData.setBodyContent(null);
        caseData.setDirectionNoticeContent(null);
        caseData.setPreviewDocument(null);
        caseData.setGenerateNotice(null);
        caseData.setReservedToJudge(null);
        caseData.setGenerateNotice(null);
        caseData.setSignedBy(null);
        caseData.setSignedRole(null);
        caseData.setDateAdded(null);
        caseData.setTempNoteDetail(null);
        caseData.setShowRip1DocPage(null);
    }
}
