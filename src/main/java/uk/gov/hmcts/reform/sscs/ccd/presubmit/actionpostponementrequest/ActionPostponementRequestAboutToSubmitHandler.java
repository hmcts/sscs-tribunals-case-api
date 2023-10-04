package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionpostponementrequest;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNull;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.POSTPONEMENT_REQUEST_DIRECTION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction.GRANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction.REFUSE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction.REFUSE_ON_THE_DAY;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction.SEND_TO_JUDGE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.PostponementRequestService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;
import uk.gov.hmcts.reform.sscs.util.SscsUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class ActionPostponementRequestAboutToSubmitHandler implements PreSubmitCallbackHandler<SscsCaseData> {

    public static final String POSTPONEMENT_DETAILS_SENT_TO_JUDGE_PREFIX = "Postponement sent to judge - ";

    private final UserDetailsService userDetailsService;
    private final PostponementRequestService postponementRequestService;
    private final FooterService footerService;
    private final ListAssistHearingMessageHelper hearingMessageHelper;
    private final IdamService idamService;
    @Value("${feature.snl.enabled}")
    private boolean isScheduleListingEnabled;

    @Override
    public boolean canHandle(CallbackType callbackType, Callback<SscsCaseData> callback) {
        requireNonNull(callback, "callback must not be null");
        requireNonNull(callbackType, "callbacktype must not be null");

        return callbackType.equals(CallbackType.ABOUT_TO_SUBMIT)
                && callback.getEvent() == EventType.ACTION_POSTPONEMENT_REQUEST
                && isScheduleListingEnabled;
    }

    @Override
    public PreSubmitCallbackResponse<SscsCaseData> handle(CallbackType callbackType, Callback<SscsCaseData> callback,
                                                          String userAuthorisation) {

        CaseDetails<SscsCaseData> caseDetails = callback.getCaseDetails();
        SscsCaseData sscsCaseData = caseDetails.getCaseData();

        PreSubmitCallbackResponse<SscsCaseData> response = new PreSubmitCallbackResponse<>(sscsCaseData);

        String caseId = sscsCaseData.getCcdCaseId();
        if (!SscsUtil.isSAndLCase(sscsCaseData)) {
            log.info("Action postponement request: Cannot process non Scheduling & Listing Case for Case ID {}",
                caseId);
            response.addError("Cannot process Action postponement request on non Scheduling & Listing Case");
            return response;
        }

        String actionRequested = sscsCaseData.getPostponementRequest().getActionPostponementRequestSelected();
        log.info("Action postponement request: handling action {} for case {}", actionRequested, caseId);
        if (SEND_TO_JUDGE.getValue().equals(actionRequested)) {
            sendToJudge(userAuthorisation, sscsCaseData);
        } else if (REFUSE.getValue().equals(actionRequested)) {
            refusePostponement(sscsCaseData);
        } else if (GRANT.getValue().equals(actionRequested)) {
            grantPostponement(sscsCaseData, response);
        } else if (REFUSE_ON_THE_DAY.getValue().equals(actionRequested)) {
            refuseOnTheDay(sscsCaseData);
        } else {
            log.info("Action postponement request: unhandled requested action {} for case {}", actionRequested,
                caseId);
            response.addError(String.format("Unhandleable Postponement Request %s", actionRequested));
        }

        clearTransientFields(sscsCaseData);

        return response;
    }

    private void refusePostponement(SscsCaseData sscsCaseData) {
        sscsCaseData.setInterlocReferralReason(null);
        sscsCaseData.setInterlocReviewState(null);
        sscsCaseData.setDwpState(DwpState.DIRECTION_ACTION_REQUIRED);
        addDirectionNotice(sscsCaseData);
        sscsCaseData.getPostponementRequest().setUnprocessedPostponementRequest(NO);
    }

    private void grantPostponement(SscsCaseData sscsCaseData, PreSubmitCallbackResponse<SscsCaseData> response) {
        cancelHearing(sscsCaseData);
        postponementRequestService.addCurrentHearingToExcludeDates(response);
        sscsCaseData.setInterlocReferralReason(null);
        sscsCaseData.setInterlocReviewState(null);
        sscsCaseData.setDwpState(DwpState.HEARING_POSTPONED);
        addDirectionNotice(sscsCaseData);

        String listingOption = sscsCaseData.getPostponementRequest().getListingOption();
        EventType postponementEventType = EventType.getEventTypeByCcdType(listingOption);
        log.info("Action postponement request: postponement listingOption {} mapped to Event {} for case {}",
            listingOption, postponementEventType, sscsCaseData.getCcdCaseId());

        sscsCaseData.setState(State.getById(listingOption));

        sscsCaseData.setPostponement(Postponement.builder()
            .unprocessedPostponement(YES)
            .postponementEvent(postponementEventType)
            .build());

        sscsCaseData.getPostponementRequest().setUnprocessedPostponementRequest(NO);
    }

    private void refuseOnTheDay(SscsCaseData sscsCaseData) {

        SscsDocument postponementDocument = getLatestPostponementDocumentForDwpType(sscsCaseData.getSscsDocument());

        if (UploadParty.DWP.equals(postponementDocument.getValue().getPartyUploaded())) {
            sscsCaseData.setDwpState(null);
            sscsCaseData.setInterlocReviewState(null);
            sscsCaseData.setState(State.HEARING);
            sscsCaseData.getPostponementRequest().setUnprocessedPostponementRequest(NO);
        }
        sscsCaseData.getPostponementRequest().setUnprocessedPostponementRequest(NO);
    }

    private void cancelHearing(SscsCaseData sscsCaseData) {
        log.info("Action postponement request: Sending cancel hearing request for case {}", sscsCaseData
            .getCcdCaseId());
        hearingMessageHelper.sendListAssistCancelHearingMessage(sscsCaseData.getCcdCaseId(),
            CancellationReason.OTHER);
    }

    private void sendToJudge(String userAuthorisation, SscsCaseData sscsCaseData) {
        if (sscsCaseData.getAppealNotePad() == null) {
            sscsCaseData.setAppealNotePad(NotePad.builder().notesCollection(new ArrayList<>()).build());
        }
        sscsCaseData.getAppealNotePad().getNotesCollection()
                .add(createPostponementRequestNote(userAuthorisation,
                        sscsCaseData.getPostponementRequest().getPostponementRequestDetails()));
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST);
        sscsCaseData.getPostponementRequest().setUnprocessedPostponementRequest(YES);
    }

    private void addDirectionNotice(SscsCaseData caseData) {
        SscsUtil.addDocumentToDocumentTabAndBundle(footerService, caseData,
                caseData.getDocumentStaging().getPreviewDocument(),
                POSTPONEMENT_REQUEST_DIRECTION_NOTICE);
    }

    private Note createPostponementRequestNote(String userAuthorisation, String details) {
        return Note.builder()
            .value(NoteDetails.builder()
                .noteDetail(POSTPONEMENT_DETAILS_SENT_TO_JUDGE_PREFIX + details)
                .author(userDetailsService.buildLoggedInUserName(userAuthorisation))
                .noteDate(LocalDate.now().toString())
                .build())
            .build();
    }

    private void clearTransientFields(SscsCaseData caseData) {
        caseData.setDocumentGeneration(DocumentGeneration.builder().build());
        caseData.setDocumentStaging(DocumentStaging.builder().build());
        caseData.setReservedToJudge(null);
        caseData.setTempNoteDetail(null);
        caseData.setShowRip1DocPage(null);

        YesNo unprocessedPostponementRequest = caseData.getPostponementRequest().getUnprocessedPostponementRequest();
        caseData.setPostponementRequest(PostponementRequest.builder()
            .unprocessedPostponementRequest(unprocessedPostponementRequest)
            .build());
    }

    private SscsDocument getLatestPostponementDocumentForDwpType(List<SscsDocument> postponementDocuments) {
        Stream<SscsDocument> filteredStream = postponementDocuments.stream()
                .filter(f -> DocumentType.POSTPONEMENT_REQUEST.getValue().equals(f.getValue().getDocumentType())
                        && !isNull(f.getValue().getOriginalPartySender()));

        List<SscsDocument> sortedList = filteredStream.sorted(Comparator.comparing(o -> o.getValue().getDocumentDateAdded()))
                .toList();

        return sortedList.get(sortedList.size() - 1);

    }
}
