package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionpostponementrequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.POSTPONEMENT_REQUEST_DIRECTION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus.TRANSLATION_REQUIRED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.WELSH_TRANSLATION;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionpostponementrequest.ActionPostponementRequestAboutToSubmitHandler.POSTPONEMENT_DETAILS_SENT_TO_JUDGE_PREFIX;

import java.time.LocalDate;
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

    private static final String DOCUMENT_URL = "dm-store/documents/123";

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

    private SscsDocument expectedDocument;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new ActionPostponementRequestAboutToSubmitHandler(userDetailsService, footerService);

        when(callback.getEvent()).thenReturn(EventType.ACTION_POSTPONEMENT_REQUEST);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
                .sscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder().build()).build();
        sscsCaseData.setAppeal(Appeal.builder().hearingOptions(HearingOptions.builder().build()).build());

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
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
                .anyMatch(note -> note.getValue().getNoteDetail()
                        .equals(POSTPONEMENT_DETAILS_SENT_TO_JUDGE_PREFIX + "Request Detail Test")), is(true));
    }

    @Test
    public void givenARefusedPostponement_thenClearReviewStateAndReferralReasonAndFlagAndAndStateIsUnchangedAddNoteAndDwpStateAndDecisionDocAdded() {
        populatePostponementSscsCaseData();

        sscsCaseData.setState(State.HEARING);
        sscsCaseData.setPostponementRequest(PostponementRequest.builder().actionPostponementRequestSelected("refuse")
                .build());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(),
                eq(POSTPONEMENT_REQUEST_DIRECTION_NOTICE), any(), any(), eq(null), eq(null));
        assertThat(response.getData().getInterlocReviewState(), is(nullValue()));
        assertThat(response.getData().getInterlocReferralReason(), is(nullValue()));
        assertThat(response.getData().getState(), is(State.HEARING));
        assertThat(response.getData().getSscsDocument(), is(not(empty())));
        assertThat(response.getData().getPostponementRequest().getUnprocessedPostponementRequest(), is(YesNo.NO));
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED.getId()));
    }

    @Test
    public void givenAGrantedPostponementAndReadyToList_thenClearReviewStateAndReferralReasonAndFlagAndAddNoteAndDwpStateAndDecisionDocAdded() {
        populatePostponementSscsCaseData();

        sscsCaseData.setPostponementRequest(PostponementRequest.builder().actionPostponementRequestSelected("grant")
                .listingOption("readyToList").build());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(),
                eq(POSTPONEMENT_REQUEST_DIRECTION_NOTICE), any(), any(), eq(null), eq(null));
        assertThat(response.getData().getInterlocReviewState(), is(nullValue()));
        assertThat(response.getData().getInterlocReferralReason(), is(nullValue()));
        assertThat(response.getData().getState(), is(State.READY_TO_LIST));
        assertThat(response.getData().getSscsDocument(), is(not(empty())));
        assertThat(response.getData().getPostponementRequest().getUnprocessedPostponementRequest(), is(YesNo.NO));
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED.getId()));
    }

    @Test
    public void givenAGrantedPostponementAndNotListable_thenClearReviewStateAndReferralReasonAndFlagAndAddNoteAndDwpStateAndDecisionDocAdded() {
        populatePostponementSscsCaseData();

        sscsCaseData.setPostponementRequest(PostponementRequest.builder().actionPostponementRequestSelected("grant")
                .listingOption("notListable").build());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(),
                eq(POSTPONEMENT_REQUEST_DIRECTION_NOTICE), any(), any(), eq(null), eq(null));
        assertThat(response.getData().getInterlocReviewState(), is(nullValue()));
        assertThat(response.getData().getInterlocReferralReason(), is(nullValue()));
        assertThat(response.getData().getState(), is(State.NOT_LISTABLE));
        assertThat(response.getData().getPostponementRequest().getUnprocessedPostponementRequest(), is(YesNo.NO));
        assertThat(response.getData().getDwpState(), is(DwpState.DIRECTION_ACTION_REQUIRED.getId()));
    }

    @Test
    public void givenDirectionNoticeAddedToWelshCase_thenSetInterlocReviewStateAndTranslationRequiredFlag() {
        populatePostponementSscsCaseData();

        sscsCaseData.setState(State.HEARING);
        sscsCaseData.setPostponementRequest(PostponementRequest.builder().actionPostponementRequestSelected("refuse")
                .build());
        sscsCaseData.setLanguagePreferenceWelsh(YES.getValue());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(),
                eq(POSTPONEMENT_REQUEST_DIRECTION_NOTICE), any(), any(), eq(null), eq(TRANSLATION_REQUIRED));
        assertThat(response.getData().getInterlocReviewState(), is(WELSH_TRANSLATION.getId()));
        assertThat(response.getData().getTranslationWorkOutstanding(), is(YES.getValue()));
        assertThat(response.getData().getSscsDocument(), is(not(empty())));
    }

    private void populatePostponementSscsCaseData() {
        sscsCaseData.setHearings(Arrays.asList(Hearing.builder().value(HearingDetails.builder()
                .hearingDate(LocalDate.now().plusDays(1).toString())
                .build()).build()));
        sscsCaseData.setPreviewDocument(DocumentLink.builder()
                .documentUrl(DOCUMENT_URL)
                .documentBinaryUrl(DOCUMENT_URL + "/binary")
                .documentFilename("directionIssued.pdf")
                .build());
        sscsCaseData.setInterlocReferralReason(InterlocReviewState.REVIEW_BY_JUDGE.getId());
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST.getId());

        expectedDocument = SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentFileName(sscsCaseData.getPreviewDocument().getDocumentFilename())
                        .documentLink(sscsCaseData.getPreviewDocument())
                        .documentDateAdded(LocalDate.now().minusDays(1).toString())
                        .documentType(POSTPONEMENT_REQUEST_DIRECTION_NOTICE.getValue())
                        .build()).build();
    }
}
