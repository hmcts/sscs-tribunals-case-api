package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionpostponementrequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;


@RunWith(JUnitParamsRunner.class)
public class ActionPostponementRequestAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    ActionPostponementRequestAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private FooterService footerService;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new ActionPostponementRequestAboutToSubmitHandler(userDetailsService, footerService);

        when(callback.getEvent()).thenReturn(EventType.PROCESS_HEARING_RECORDING_REQUEST);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
                .sscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder().build()).build();
        sscsCaseData.setAppeal(Appeal.builder().hearingOptions(HearingOptions.builder().build()).build());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenASendToJudge_thenSetReviewStateAndReferralReasonAndAddNote() {
        sscsCaseData.setPostponementRequest(PostponementRequest.builder()
                .actionPostponementRequestSelected("sendToJudge")
                .postponementRequestDetails("Request Detail Test").build());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(),
                is(InterlocReviewState.REVIEW_BY_JUDGE.getId()));
        assertThat(response.getData().getInterlocReferralReason(),
                is(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST.getId()));
        assertThat(response.getData().getPostponementRequest().getUnprocessedPostponementRequest(), is(YesNo.YES));
        assertThat(response.getData().getAppealNotePad().getNotesCollection().stream()
                .anyMatch(note -> note.getValue().getNoteDetail().equals("Request Detail Test")), is(true));
    }

    @Test
    public void givenAGrantedPostponementAndReadyToList_thenSetReviewStateAndReferralReasonAndFlagAndAddNoteAndUnavailabilityUpdatedAndDwpStateAndDecisionDocAdded() {
        String hearingDate = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder().hearingDate(hearingDate)
                .build()).build()));
        sscsCaseData.setPostponementRequest(PostponementRequest.builder().actionPostponementRequestSelected("grant")
                .listingOption("readyToList").build());
        sscsCaseData.setPreviewDocument(DocumentLink.builder().build());
        sscsCaseData.setInterlocReferralReason(InterlocReviewState.REVIEW_BY_JUDGE.getId());
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST.getId());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(nullValue()));
        assertThat(response.getData().getInterlocReferralReason(), is(nullValue()));
        assertThat(response.getData().getState(), is(State.READY_TO_LIST));
        assertThat(response.getData().getAppeal().getHearingOptions().getExcludeDates().stream()
                        .anyMatch(excludeDate -> excludeDate.getValue().getStart().equals(hearingDate)),
                is(true));
        assertThat(response.getData().getSscsDocument(), is(not(empty())));
        assertThat(response.getData().getPostponementRequest().getUnprocessedPostponementRequest(), is(YesNo.NO));
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED.getId()));
    }

    @Test
    public void givenAGrantedPostponementAndNotListable_thenSetReviewStateAndReferralReasonAndFlagAndAddNoteAndUnavailabilityUpdatedAndDwpStateAndDecisionDocAdded() {
        String hearingDate = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder().hearingDate(hearingDate)
                .build()).build()));
        sscsCaseData.setPostponementRequest(PostponementRequest.builder().actionPostponementRequestSelected("grant")
                .listingOption("notListable").build());
        sscsCaseData.setPreviewDocument(DocumentLink.builder().build());
        sscsCaseData.setInterlocReferralReason(InterlocReviewState.REVIEW_BY_JUDGE.getId());
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST.getId());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(nullValue()));
        assertThat(response.getData().getInterlocReferralReason(), is(nullValue()));
        assertThat(response.getData().getState(), is(State.NOT_LISTABLE));
        assertThat(response.getData().getAppeal().getHearingOptions().getExcludeDates().stream()
                        .anyMatch(excludeDate -> excludeDate.getValue().getStart().equals(hearingDate)),
                is(true));
        assertThat(response.getData().getSscsDocument(), is(not(empty())));
        assertThat(response.getData().getPostponementRequest().getUnprocessedPostponementRequest(), is(YesNo.NO));
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED.getId()));
    }
}