package uk.gov.hmcts.reform.sscs.ccd.presubmit.decisionissued;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DecisionType.STRIKE_OUT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.STRUCK_OUT;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentLink;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentTranslationStatus;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsInterlocDecisionDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.service.EvidenceManagementSecureDocStoreService;
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
    private EvidenceManagementSecureDocStoreService evidenceManagementService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private CaseDetails<SscsCaseData> caseDetailsBefore;

    private SscsCaseData sscsCaseData;

    private SscsDocument expectedDocument;

    private SscsWelshDocument expectedWelshDocument;

    @Before
    public void setUp() {
        handler = new DecisionIssuedAboutToSubmitHandler(footerService);
        when(callback.getEvent()).thenReturn(EventType.DECISION_ISSUED);

        SscsDocument document = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("myTest.doc").build()).build();
        List<SscsDocument> docs = new ArrayList<>();
        docs.add(document);

        sscsCaseData = SscsCaseData.builder()
                .state(State.INTERLOCUTORY_REVIEW_STATE)
                .generateNotice("Yes")
                .signedBy("User")
                .signedRole("Judge")
                .dateAdded(LocalDate.now().minusDays(1))
                .sscsDocument(docs)
                .decisionType(STRIKE_OUT.getValue())
                .directionDueDate("01/02/2020")
                .previewDocument(DocumentLink.builder()
                        .documentUrl(DOCUMENT_URL)
                        .documentBinaryUrl(DOCUMENT_URL + "/binary")
                        .documentFilename("decisionIssued.pdf")
                        .build())
                .appeal(Appeal.builder()
                        .appellant(Appellant.builder()
                                .name(Name.builder().build())
                                .identity(Identity.builder().build())
                                .build())
                        .build()).build();

        expectedDocument = SscsDocument.builder()
                .value(SscsDocumentDetails.builder()
                        .documentFileName(sscsCaseData.getPreviewDocument().getDocumentFilename())
                        .documentLink(sscsCaseData.getPreviewDocument())
                        .documentDateAdded(LocalDate.now().minusDays(1).toString())
                        .documentType(DocumentType.DECISION_NOTICE.getValue())
                        .build()).build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetailsBefore.getState()).thenReturn(State.INTERLOCUTORY_REVIEW_STATE);
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
    @Parameters({"strikeOut, struckOut, null", "null, struckOut, interlocutoryReviewState", ",struckOut, interlocutoryReviewState"})
    public void givenDecisionTypeIsStrikeOut_shouldSetDwpStateValueAndInterlocReviewState(@Nullable String decisionType,
                                                                                          @Nullable String expectedDwpState,
                                                                                          @Nullable String expectedInterlocReviewState) {
        sscsCaseData.setDecisionType(decisionType);
        sscsCaseData.setInterlocReviewState(State.INTERLOCUTORY_REVIEW_STATE.getId());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        String currentDwpState = response.getData().getDwpState();
        String assertionMsg = "dwpState value (%s) is not as expected (%s)";
        assertEquals(String.format(assertionMsg, currentDwpState, expectedDwpState), expectedDwpState, currentDwpState);
        assertEquals(expectedInterlocReviewState, response.getData().getInterlocReviewState());
        assertNull(response.getData().getDirectionDueDate());
    }

    @Test
    public void willCopyThePreviewFileToTheInterlocDecisionDocumentAndAddFooter() {
        final PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertNull(response.getData().getPreviewDocument());
        assertNull(response.getData().getSignedBy());
        assertNull(response.getData().getSignedRole());
        assertNull(response.getData().getGenerateNotice());
        assertNull(response.getData().getDateAdded());

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()),
                any(), eq(DocumentType.DECISION_NOTICE), any(), any(), eq(null), eq(null));
    }

    @Test
    public void given2ManuallyUploadedDecisionDocumentsOneWithNoDate_thenIssueDocumentWithNoDate() {
        sscsCaseData.setPreviewDocument(null);

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
        sscsCaseData.setInterlocReviewState(State.INTERLOCUTORY_REVIEW_STATE.getId());
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
        assertEquals(STRUCK_OUT.getId(), response.getData().getDwpState());
        assertEquals("nonCompliantAppealStruckout", response.getData().getOutcome());
        assertNull(response.getData().getInterlocReviewState());
        assertEquals(response.getData().getState(), (State.DORMANT_APPEAL_STATE));
    }

    @Test
    public void givenDecisionIssuedAndCaseIsPostValidInterloc_setDwpStateAndOutcomeToStruckOut() {
        when(caseDetailsBefore.getState()).thenReturn(State.WITH_DWP);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(STRUCK_OUT.getId(), response.getData().getDwpState());
        assertEquals("struckOut", response.getData().getOutcome());
        assertEquals(response.getData().getState(), (State.DORMANT_APPEAL_STATE));
    }

    @Test
    public void givenDecisionIssuedAndCaseIsWelsh_DoNotSetDwpStateAndOutcomeToStruckOut() {
        when(caseDetailsBefore.getState()).thenReturn(State.WITH_DWP);
        sscsCaseData.setLanguagePreferenceWelsh("Yes");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("welshTranslation", response.getData().getInterlocReviewState());
        assertEquals("Yes", response.getData().getTranslationWorkOutstanding());
        assertNull(response.getData().getDwpState());
        assertNull(response.getData().getOutcome());
        assertEquals(response.getData().getState(), (State.INTERLOCUTORY_REVIEW_STATE));

        verify(footerService).createFooterAndAddDocToCase(eq(expectedDocument.getValue().getDocumentLink()),
                any(), eq(DocumentType.DECISION_NOTICE), any(), any(), eq(null), eq(SscsDocumentTranslationStatus.TRANSLATION_REQUIRED));
    }

    @Test
    public void givenDecisionIssuedWelshAndCaseIsWelsh_SetDwpStateAndOutcomeToStruckOut() {

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
        assertEquals(STRUCK_OUT.getId(), response.getData().getDwpState());
        assertEquals("struckOut", response.getData().getOutcome());
        assertEquals(response.getData().getState(), (State.DORMANT_APPEAL_STATE));

        verifyNoInteractions(footerService);
    }

    @Test
    public void givenNoPdfIsUploaded_thenAddAnErrorToResponse() {
        sscsCaseData.setPreviewDocument(null);

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
        sscsCaseData.setPreviewDocument(null);

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
}
