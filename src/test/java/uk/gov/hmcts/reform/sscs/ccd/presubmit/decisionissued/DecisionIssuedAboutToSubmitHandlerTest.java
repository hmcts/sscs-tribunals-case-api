package uk.gov.hmcts.reform.sscs.ccd.presubmit.decisionissued;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DecisionType.STRIKE_OUT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.STRUCK_OUT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.WELSH_TRANSLATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.resendtogaps.ListAssistHearingMessageHelper;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;
import uk.gov.hmcts.reform.sscs.service.FooterService;

@ExtendWith(MockitoExtension.class)
public class DecisionIssuedAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private static final String DOCUMENT_URL = "dm-store/documents/123";
    private static final String DOCUMENT_URL2 = "dm-store/documents/456";

    @Mock
    private FooterService footerService;

    private DecisionIssuedAboutToSubmitHandler handler;

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

    @BeforeEach
    public void setUp() {
        handler = new DecisionIssuedAboutToSubmitHandler(footerService, hearingMessageHelper, false, false, false);

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
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"APPEAL_RECEIVED", "ACTION_FURTHER_EVIDENCE"})
    public void givenANonHandleEvidenceEvent_thenReturnFalse(EventType eventType) {
        when(callback.getEvent()).thenReturn(eventType);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @ParameterizedTest
    @EnumSource(value = EventType.class, names = {"DECISION_ISSUED", "DECISION_ISSUED_WELSH"})
    public void givenAValidHandleAndEventType_thenReturnTrue(EventType eventType) {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getEvent()).thenReturn(eventType);
        assertTrue(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @ParameterizedTest
    @EnumSource(value = CallbackType.class, names = {"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @ParameterizedTest
    @EnumSource(names = {"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    void givenANonValidCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertThrows(IllegalStateException.class, () -> handler.handle(callbackType, callback, USER_AUTHORISATION));
    }

    @ParameterizedTest
    @CsvSource({"strikeOut, STRUCK_OUT,", "null, STRUCK_OUT, REVIEW_BY_TCW", ",STRUCK_OUT, REVIEW_BY_TCW"})
    public void givenDecisionTypeIsStrikeOut_shouldSetDwpStateValueAndInterlocReviewState(String decisionType,
                                                                                          DwpState expectedDwpState,
                                                                                          InterlocReviewState expectedInterlocReviewState) {
        when(callback.getEvent()).thenReturn(EventType.DECISION_ISSUED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        sscsCaseData.setDecisionType(decisionType);
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_TCW);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        DwpState currentDwpState = response.getData().getDwpState();
        String assertionMsg = "dwpState value (%s) is not as expected (%s)";
        assertEquals(expectedDwpState, currentDwpState, assertionMsg.formatted(currentDwpState, expectedDwpState));
        assertEquals(expectedInterlocReviewState, response.getData().getInterlocReviewState());
        assertNull(response.getData().getDirectionDueDate());
        verifyNoInteractions(hearingMessageHelper);
    }

    @Test
    public void willCopyThePreviewFileToTheInterlocDecisionDocumentAndAddFooter() {
        when(callback.getEvent()).thenReturn(EventType.DECISION_ISSUED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetailsBefore.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE).thenReturn(HEARING);
        handler = new DecisionIssuedAboutToSubmitHandler(footerService, hearingMessageHelper, true, false, false);

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
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getEvent()).thenReturn(EventType.DECISION_ISSUED);
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

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        verify(footerService).createFooterAndAddDocToCase(eq(theDocument.getDocumentLink()), any(), eq(DocumentType.DECISION_NOTICE), any(), eq(theDocument.getDocumentDateAdded()), eq(null), eq(null));
    }

    @Test
    public void givenDecisionIssuedAndCaseIsPreValidInterloc_setDwpStateToStruckOutAndOutcomeToNonCompliantAppealStruckout() {
        when(callback.getEvent()).thenReturn(EventType.DECISION_ISSUED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetailsBefore.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE).thenReturn(HEARING);
        sscsCaseData.setInterlocReviewState(InterlocReviewState.REVIEW_BY_TCW);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(STRUCK_OUT, response.getData().getDwpState());
        assertEquals("nonCompliantAppealStruckout", response.getData().getOutcome());
        assertNull(response.getData().getInterlocReviewState());
        assertEquals(response.getData().getState(), (State.DORMANT_APPEAL_STATE));
    }

    @Test
    public void givenDecisionIssuedAndCaseIsPostValidInterloc_setDwpStateAndOutcomeToStruckOut() {
        when(callback.getEvent()).thenReturn(EventType.DECISION_ISSUED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetailsBefore.getState()).thenReturn(State.WITH_DWP);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(STRUCK_OUT, response.getData().getDwpState());
        assertEquals("struckOut", response.getData().getOutcome());
        assertEquals(response.getData().getState(), (State.DORMANT_APPEAL_STATE));
    }

    @Test
    public void givenDecisionIssuedAndCaseIsWelsh_DoNotSetDwpStateAndOutcomeToStruckOut() {
        when(callback.getEvent()).thenReturn(EventType.DECISION_ISSUED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
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
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        handler = new DecisionIssuedAboutToSubmitHandler(footerService, hearingMessageHelper, true, false, false);
        SscsWelshDocument expectedWelshDocument = SscsWelshDocument.builder()
                .value(SscsWelshDocumentDetails.builder()
                        .documentFileName("welshDecisionDocFilename")
                        .documentLink(DocumentLink.builder().documentUrl("welshUrl").documentBinaryUrl("welshBinaryUrl").build())
                        .documentDateAdded(LocalDate.now().minusDays(1).toString())
                        .documentType(DocumentType.DECISION_NOTICE.getValue())
                        .build()).build();
        sscsCaseData.setSscsWelshDocuments(new ArrayList<>());
        sscsCaseData.getSscsWelshDocuments().add(expectedWelshDocument);


        when(callback.getEvent()).thenReturn(EventType.DECISION_ISSUED_WELSH);
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
        when(callback.getEvent()).thenReturn(EventType.DECISION_ISSUED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.getDocumentStaging().setPreviewDocument(null);
        sscsCaseData.setSscsInterlocDecisionDocument(null);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(1, response.getErrors().size());

        for (String error : response.getErrors()) {
            assertEquals("You need to upload a PDF document", error);
        }
    }

    @ParameterizedTest
    @CsvSource({"file.png", "file.jpg", "file.doc"})
    public void givenManuallyUploadedFileIsNotAPdf_thenAddAnErrorToResponse(String filename) {
        when(callback.getEvent()).thenReturn(EventType.DECISION_ISSUED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
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
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getEvent()).thenReturn(EventType.DECISION_ISSUED);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.REVIEW_CORRECTION_APPLICATION);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(InterlocReferralReason.REVIEW_CORRECTION_APPLICATION, response.getData().getInterlocReferralReason());
    }

    @Test
    public void shouldClearInterlocReferralReasonIfPostHearingsEnabled() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(callback.getEvent()).thenReturn(EventType.DECISION_ISSUED);
        handler = new DecisionIssuedAboutToSubmitHandler(footerService, hearingMessageHelper, true, true, false);
        sscsCaseData.setInterlocReferralReason(InterlocReferralReason.REVIEW_CORRECTION_APPLICATION);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNull(response.getData().getInterlocReferralReason());
    }

    @Test
    public void shouldSetIssueDecisionDateWhenWorkAllocationEnabled() {
        when(callback.getEvent()).thenReturn(EventType.DECISION_ISSUED);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        handler = new DecisionIssuedAboutToSubmitHandler(footerService, hearingMessageHelper, false, false, true);

        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(LocalDate.now(), response.getData().getIssueInterlocDecisionDate());
    }
}
