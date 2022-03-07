package uk.gov.hmcts.reform.sscs.ccd.presubmit.actionpostponementrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.POSTPONEMENT_REQUEST_DIRECTION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus.TRANSLATION_REQUIRED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState.WELSH_TRANSLATION;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.actionpostponementrequest.ActionPostponementRequestAboutToSubmitHandler.POSTPONEMENT_DETAILS_SENT_TO_JUDGE_PREFIX;

import java.time.LocalDate;
import java.util.List;
import junitparams.JUnitParamsRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.DwpState;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Hearing;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.Note;
import uk.gov.hmcts.reform.sscs.ccd.domain.NoteDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Postponement;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostponementRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.SchedulingAndListingFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsHearingRecordingCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReferralReason;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.service.FooterService;
import uk.gov.hmcts.reform.sscs.service.PostponementRequestService;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;

@RunWith(JUnitParamsRunner.class)
public class ActionPostponementRequestAboutToSubmitHandlerTest {

    private static final String DOCUMENT_URL = "dm-store/documents/123";

    private static final String USER_AUTHORISATION = "Bearer token";

    @InjectMocks
    ActionPostponementRequestAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private FooterService footerService;

    @Mock
    private ListAssistHearingMessageHelper hearingMessageHelper;

    @Mock
    private PostponementRequestService postponementRequestService;

    private SscsCaseData sscsCaseData;

    private SscsDocument expectedDocument;

    @Before
    public void setUp() {
        openMocks(this);
        ReflectionTestUtils.setField(handler, "isScheduleListingEnabled", true);

        when(callback.getEvent()).thenReturn(EventType.ACTION_POSTPONEMENT_REQUEST);
        when(callback.getCaseDetails()).thenReturn(caseDetails);

        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .appeal(Appeal.builder().hearingOptions(HearingOptions.builder().build()).build())
            .directionNoticeContent("Body Content")
            .sscsHearingRecordingCaseData(SscsHearingRecordingCaseData.builder().build())
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                .hearingRoute(HearingRoute.LIST_ASSIST).build())
            .hearings(List.of(Hearing.builder()
                .value(HearingDetails.builder()
                    .hearingDate(LocalDate.now().plusDays(1).toString())
                    .time("10:00")
                    .build())
                .build()))
            .previewDocument(DocumentLink.builder()
                .documentUrl(DOCUMENT_URL)
                .documentBinaryUrl(DOCUMENT_URL + "/binary")
                .documentFilename("directionIssued.pdf")
                .build())
            .interlocReferralReason(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST.getId())
            .postponementRequest(PostponementRequest.builder()
                .actionPostponementRequestSelected("grant")
                .build())
            .build();

        expectedDocument = SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentFileName(sscsCaseData.getPreviewDocument().getDocumentFilename())
                .documentLink(sscsCaseData.getPreviewDocument())
                .documentDateAdded(LocalDate.now().minusDays(1).toString())
                .documentType(POSTPONEMENT_REQUEST_DIRECTION_NOTICE.getValue())
                .build()).build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenAValidAboutToSubmitEvent_thenReturnTrue() {
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isTrue();
    }

    @Test
    public void givenSchedulingAndListingEnabledFalse_thenReturnFalse() {
        ReflectionTestUtils.setField(handler, "isScheduleListingEnabled", false);
        assertThat(handler.canHandle(ABOUT_TO_SUBMIT, callback)).isFalse();
    }

    @Test
    public void givenASendToJudge_thenSetReviewStateAndReferralReasonAndAddNote() {
        sscsCaseData.setPostponementRequest(PostponementRequest.builder()
                .actionPostponementRequestSelected("sendToJudge")
                .postponementRequestDetails("Request Detail Test").build());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState())
            .isEqualTo(InterlocReviewState.REVIEW_BY_JUDGE.getId());
        assertThat(response.getData().getInterlocReferralReason())
            .isEqualTo(InterlocReferralReason.REVIEW_POSTPONEMENT_REQUEST.getId());
        assertThat(response.getData().getPostponementRequest().getUnprocessedPostponementRequest()).isEqualTo(YES);
        assertThat(response.getData().getAppealNotePad().getNotesCollection())
            .isNotEmpty()
            .extracting(Note::getValue)
            .extracting(NoteDetails::getNoteDetail)
            .contains(POSTPONEMENT_DETAILS_SENT_TO_JUDGE_PREFIX + "Request Detail Test");
    }

    @Test
    public void givenARefusedPostponement_thenClearReviewStateAndReferralReasonAndFlagAndAddNoteAndDwpStateAndDecisionDocAdded() {
        sscsCaseData.setPostponementRequest(PostponementRequest.builder()
            .actionPostponementRequestSelected("refuse")
            .build());

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(),
                eq(POSTPONEMENT_REQUEST_DIRECTION_NOTICE), any(), any(), eq(null), eq(null));

        verifyNoInteractions(hearingMessageHelper);
        assertThat(response.getData().getInterlocReviewState()).isNull();
        assertThat(response.getData().getInterlocReferralReason()).isNull();
        assertThat(response.getData().getPostponementRequest().getUnprocessedPostponementRequest()).isEqualTo(NO);
        assertThat(response.getData().getDwpState()).isEqualTo(DwpState.DIRECTION_ACTION_REQUIRED.getId());
    }

    @Test
    public void givenAGrantedPostponement_thenClearReviewStateAndReferralReasonAndFlagAndAddNoteAndDwpStateAndDecisionDocAdded() {
        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState()).isNull();
        assertThat(response.getData().getInterlocReferralReason()).isNull();
        assertThat(response.getData().getPostponementRequest().getUnprocessedPostponementRequest()).isEqualTo(NO);
        assertThat(response.getData().getDwpState()).isEqualTo(DwpState.HEARING_POSTPONED.getId());
        assertThat(response.getData().getDirectionNoticeContent()).isNull();
        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(),
                eq(POSTPONEMENT_REQUEST_DIRECTION_NOTICE), any(), any(), eq(null), eq(null));
    }

    @Test
    public void givenAGrantedPostponement_shouldSendCancellation() {
        sscsCaseData.setAppeal(Appeal.builder().hearingOptions(HearingOptions.builder().build()).build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(hearingMessageHelper).sendListAssistCancelHearingMessage(sscsCaseData.getCcdCaseId(),
            CancellationReason.OTHER);
        verifyNoMoreInteractions(hearingMessageHelper);
    }

    @Test
    public void givenAGrantedPostponementAndReadyToList_shouldSetPostponementCorrectly() {
        sscsCaseData.setAppeal(Appeal.builder().hearingOptions(HearingOptions.builder().build()).build());
        sscsCaseData.getPostponementRequest().setListingOption("readyToList");

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);


        Postponement expected = Postponement.builder()
            .unprocessedPostponement(YES)
            .postponementEvent(EventType.READY_TO_LIST)
            .build();

        assertThat(response.getData().getPostponement())
            .isEqualTo(expected);
    }

    @Test
    public void givenAGrantedPostponementAndNotListable_shouldSetPostponementCorrectly() {

        sscsCaseData.setAppeal(Appeal.builder().hearingOptions(HearingOptions.builder().build()).build());
        sscsCaseData.getPostponementRequest().setListingOption("notListable");

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);


        Postponement expected = Postponement.builder()
            .unprocessedPostponement(YES)
            .postponementEvent(EventType.NOT_LISTABLE)
            .build();

        assertThat(response.getData().getPostponement())
            .isEqualTo(expected);
    }

    @Test
    public void givenDirectionNoticeAddedToWelshCase_thenSetInterlocReviewStateAndTranslationRequiredFlag() {
        sscsCaseData.setState(State.HEARING);
        sscsCaseData.setPostponementRequest(PostponementRequest.builder()
            .actionPostponementRequestSelected("refuse")
            .build());

        sscsCaseData.setLanguagePreferenceWelsh(YES);

        PreSubmitCallbackResponse<SscsCaseData> response =
                handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()), any(),
                eq(POSTPONEMENT_REQUEST_DIRECTION_NOTICE), any(), any(), eq(null), eq(TRANSLATION_REQUIRED));

        assertThat(response.getData().getInterlocReviewState()).isEqualTo(WELSH_TRANSLATION.getId());
        assertThat(response.getData().getTranslationWorkOutstanding()).isEqualTo(YES);
        System.out.println(response.getData().getSscsDocument());
        assertThat(response.getData().getDirectionNoticeContent()).isNull();
    }

    @Test
    public void givenNonSAndLCase_shouldNoMessages() {
        sscsCaseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder()
            .hearingRoute(HearingRoute.GAPS).build());

        PreSubmitCallbackResponse<SscsCaseData> response =
            handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verifyNoInteractions(hearingMessageHelper);
    }
}
