package uk.gov.hmcts.reform.sscs.ccd.presubmit.decisionissued;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DecisionType.STRIKE_OUT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.STRUCK_OUT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.WELSH_TRANSLATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.converters.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementService;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@RunWith(JUnitParamsRunner.class)
public class DecisionIssuedAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String DOCUMENT_URL = "dm-store/documents/123";
    private static final String DOCUMENT_URL2 = "dm-store/documents/456";

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private FooterService footerService;

    private DecisionIssuedAboutToSubmitHandler handler;

    @Mock
    private EvidenceManagementService evidenceManagementService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private CaseDetails<SscsCaseData> caseDetailsBefore;

    @Mock
    private ListAssistHearingMessageHelper hearingMessageHelper;

    private SscsCaseData sscsCaseData;

    private SscsDocument expectedDocument;

    private SscsWelshDocument expectedWelshDocument;

    @Before
    public void setUp() {
        handler = new DecisionIssuedAboutToSubmitHandler(footerService, hearingMessageHelper, false, false);
        when(callback.getEvent()).thenReturn(EventType.DECISION_ISSUED);

        SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("myTest.doc").build()).build();
        List<SscsDocument> docs = new ArrayList<>();
        docs.add(document);

        sscsCaseData = SscsCaseData.builder()
            .state(State.INTERLOCUTORY_REVIEW_STATE)
            .documentGeneration(DocumentGeneration.builder()
                .generateNotice(YES)
                .signedBy("User")
                .signedRole("Judge")
                .build())
            .documentStaging(DocumentStaging.builder()
                .dateAdded(LocalDate.now().minusDays(1))
                .previewDocument(DocumentLink.builder()
                    .documentUrl(DOCUMENT_URL)
                    .documentBinaryUrl(DOCUMENT_URL + "/binary")
                    .documentFilename("decisionIssued.pdf")
                    .build())
                .build())
            .sscsDocument(docs)
            .decisionType(STRIKE_OUT.getValue())
            .directionDueDate("01/02/2020")
            .appeal(Appeal.builder()
                    .appellant(Appellant.builder()
                            .name(Name.builder().build())
                            .identity(Identity.builder().build())
                            .build())
                    .build())
            .schedulingAndListingFields(SchedulingAndListingFields.builder()
                    .hearingRoute(HearingRoute.LIST_ASSIST)
                    .build())
            .build();

        expectedDocument = SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentFileName(sscsCaseData.getDocumentStaging().getPreviewDocument().getDocumentFilename())
                        .documentLink(sscsCaseData.getDocumentStaging().getPreviewDocument())
                        .documentDateAdded(LocalDate.now().minusDays(1).toString())
                        .documentType(DocumentType.DECISION_NOTICE.getValue())
                        .build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetailsBefore.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE).thenReturn(HEARING);
    }

    @Test
    @Parameters({"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(MID_EVENT, callback));
    }

    @Test
    @Parameters({"DECISION_ISSUED", "DECISION_ISSUED_WELSH"})
    public void givenAValidHandleAndEventType_thenReturnTrue(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test
    public void givenGenerateNoticeIsYes_thenReturnTrue() {
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({"strikeOut, STRUCK_OUT, null", "null, STRUCK_OUT, REVIEW_BY_TCW", ",STRUCK_OUT, REVIEW_BY_TCW"})
    public void givenDecisionTypeIsStrikeOut_shouldSetDwpStateValueAndInterlocReviewState(@Nullable String decisionType,
                                                                                          @Nullable DwpState expectedDwpState,
                                                                                          @Nullable InterlocReviewState expectedInterlocReviewState) {
        sscsCaseData.setDecisionType(decisionType);
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_TCW);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        DwpState currentDwpState = response.getData().getDwpState();
        String assertionMsg = "dwpState value (%s) is not as expected (%s)";
        assertEquals(String.format(assertionMsg, currentDwpState, expectedDwpState), expectedDwpState, currentDwpState);
        assertEquals(expectedInterlocReviewState, response.getData().getInterlocReviewState());
        assertNull(response.getData().getDirectionDueDate());
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    public void willCopyThePreviewFileToTheInterlocDecisionDocumentAndAddFooter() {
        handler = new DecisionIssuedAboutToSubmitHandler(footerService, hearingMessageHelper, true, false);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getDocumentStaging().getPreviewDocument());
        assertNull(response.getData().getDocumentGeneration().getSignedBy());
        assertNull(response.getData().getDocumentGeneration().getSignedRole());
        assertNull(response.getData().getDocumentGeneration().getGenerateNotice());
        assertNull(response.getData().getDocumentStaging().getDateAdded());

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()),
                any(), eq(DocumentType.DECISION_NOTICE), any(), any(), eq(null), eq(null));
        verify(hearingMessageHelper).sendListAssistCancelHearingMessage(eq(sscsCaseData.getCcdCaseId()),
                eq(CancellationReason.STRUCK_OUT));
        verifyNoMoreInteractions(hearingMessageHelper);
    }

    @Test
    public void given2ManuallyUploadedDecisionDocumentsOneWithNoDate_thenIssueDocumentWithNoDate() {
        sscsCaseData.getDocumentStaging().setPreviewDocument(null);

        List<SscsDocument> sscsDocuments = new ArrayList<>();
        SscsDocument document1 = SscsDocument.builder().value(SscsDocumentDetails.builder()
                .documentType(DocumentType.DECISION_NOTICE.getValue())
                .documentLink(DocumentLink.builder().documentFilename("file.pdf").documentUrl(DOCUMENT_URL).build()).build())
                .build();

        SscsInterlocDecisionDocument theDocument = SscsInterlocDecisionDocument.builder()
                .documentType(DocumentType.DECISION_NOTICE.getValue())
                .documentLink(DocumentLink.builder().documentFilename("file.pdf").documentUrl(DOCUMENT_URL2).build())
                .documentDateAdded(LocalDate.now()).build();

        sscsCaseData.setSscsInterlocDecisionDocument(theDocument);

        sscsDocuments.add(document1);
        sscsCaseData.setSscsDocument(sscsDocuments);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(theDocument.getDocumentLink()), any(), eq(DocumentType.DECISION_NOTICE), any(), eq(theDocument.getDocumentDateAdded()), eq(null), eq(null));
    }

    @Test
    public void givenDecisionIssuedAndCaseIsPreValidInterloc_setDwpStateToStruckOutAndOutcomeToNonCompliantAppealStruckout() {
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_TCW);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(STRUCK_OUT, response.getData().getDwpState());
        assertEquals("nonCompliantAppealStruckout", response.getData().getOutcome());
        assertNull(response.getData().getInterlocReviewState());
        assertEquals(response.getData().getState(), (State.DORMANT_APPEAL_STATE));
    }

    @Test
    public void givenDecisionIssuedAndCaseIsPostValidInterloc_setDwpStateAndOutcomeToStruckOut() {
        when(caseDetailsBefore.getState()).thenReturn(State.WITH_DWP);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(STRUCK_OUT, response.getData().getDwpState());
        assertEquals("struckOut", response.getData().getOutcome());
        assertEquals(response.getData().getState(), (State.DORMANT_APPEAL_STATE));
    }

    @Test
    public void givenDecisionIssuedAndCaseIsWelsh_DoNotSetDwpStateAndOutcomeToStruckOut() {
        when(caseDetailsBefore.getState()).thenReturn(State.WITH_DWP);
        sscsCaseData.setLanguagePreferenceWelsh("Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(WELSH_TRANSLATION, response.getData().getInterlocReviewState());
        assertEquals("Yes", response.getData().getTranslationWorkOutstanding());
        assertNull(response.getData().getDwpState());
        assertNull(response.getData().getOutcome());
        assertEquals(response.getData().getState(), (State.INTERLOCUTORY_REVIEW_STATE));

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()),
                any(), eq(DocumentType.DECISION_NOTICE), any(), any(), eq(null), eq(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED));
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    public void givenDecisionIssuedWelshAndCaseIsWelsh_SetDwpStateAndOutcomeToStruckOut() {
        handler = new DecisionIssuedAboutToSubmitHandler(footerService, hearingMessageHelper, true, false);
        expectedWelshDocument = SscsWelshDocument.builder()
                .value(SscsWelshDocumentDetails.builder()
                        .documentFileName("welshDecisionDocFilename")
                        .documentLink(DocumentLink.builder().documentUrl("welshUrl").documentBinaryUrl("welshBinaryUrl").build())
                        .documentDateAdded(LocalDate.now().minusDays(1).toString())
                        .documentType(DocumentType.DECISION_NOTICE.getValue())
                        .build()).build();
        sscsCaseData.setSscsWelshDocuments(new ArrayList<>());
        sscsCaseData.getSscsWelshDocuments().add(expectedWelshDocument);


        when(callback.getEvent()).thenReturn(EventType.DECISION_ISSUED_WELSH);
        when(caseDetailsBefore.getState()).thenReturn(State.WITH_DWP);
        sscsCaseData.setLanguagePreferenceWelsh("Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getInterlocReviewState());
        assertEquals(STRUCK_OUT, response.getData().getDwpState());
        assertEquals("struckOut", response.getData().getOutcome());
        assertEquals(response.getData().getState(), (State.DORMANT_APPEAL_STATE));

        verifyNoInteractions(footerService);
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    public void givenNoPdfIsUploaded_thenAddAnErrorToResponse() {
        sscsCaseData.getDocumentStaging().setPreviewDocument(null);

        sscsCaseData.setSscsInterlocDecisionDocument(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("You need to upload a PDF document", error);
        }
    }

    @Test
    @Parameters({"file.png", "file.jpg", "file.doc"})
    public void givenManuallyUploadedFileIsNotAPdf_thenAddAnErrorToResponse(String filename) {
        sscsCaseData.getDocumentStaging().setPreviewDocument(null);

        SscsInterlocDecisionDocument theDocument = SscsInterlocDecisionDocument.builder()
                .documentType(DocumentType.DECISION_NOTICE.getValue())
                .documentLink(DocumentLink.builder().documentFilename(filename).documentUrl(DOCUMENT_URL).build())
                .documentDateAdded(LocalDate.now()).build();

        sscsCaseData.setSscsInterlocDecisionDocument(theDocument);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("You need to upload PDF documents only", error);
        }
    }

    @Test
    public void shouldNotClearInterlocReferralReasonIfPostHearingsNotEnabled() {
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.REVIEW_CORRECTION_APPLICATION);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(InterlocReferralReason.REVIEW_CORRECTION_APPLICATION, response.getData().getInterlocReferralReason());
    }

    @Test
    public void shouldClearInterlocReferralReasonIfPostHearingsEnabled() {
        handler = new DecisionIssuedAboutToSubmitHandler(footerService, hearingMessageHelper, true, true);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.REVIEW_CORRECTION_APPLICATION);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getInterlocReferralReason());
    }
}
