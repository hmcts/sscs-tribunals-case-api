package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadwelshdocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPLOAD_WELSH_DOCUMENT;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(JUnitParamsRunner.class)
public class UploadWelshDocumentsSubmittedHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private UploadWelshDocumentsSubmittedHandler handler;

    @Mock
    private CcdService ccdService;
    @Mock
    private IdamService idamService;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new UploadWelshDocumentsSubmittedHandler(ccdService, idamService);
        when(callback.getEvent()).thenReturn(EventType.UPLOAD_WELSH_DOCUMENT);
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build())
            .sscsWelshPreviewNextEvent("sendToDwp")
            .build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

    }

    @Test
    @Parameters(method = "generateCanHandleScenarios")
    public void givenCanHandleIsCalled_shouldReturnCorrectResult(CallbackType callbackType,
                                                                 Callback<SscsCaseData> callback,
                                                                 boolean expectedResult) {
        boolean actualResult = handler.canHandle(callbackType, callback);
        assertEquals(expectedResult, actualResult);
    }

    @Test
    public void shouldCallUpdateCaseWithCorrectEvent() {
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
        when(ccdService.updateCase(caseData, callback.getCaseDetails().getId(), EventType.SEND_TO_DWP.getCcdType(),
            "Upload welsh document",
            "Upload welsh document", idamTokens))
            .thenReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);
        verify(ccdService).updateCase(caseData, callback.getCaseDetails().getId(), EventType.SEND_TO_DWP.getCcdType(),
            "Upload welsh document", "Upload welsh document", idamTokens);
        verify(ccdService, never()).updateCase(caseData, callback.getCaseDetails().getId(), EventType.MAKE_CASE_URGENT.getCcdType(),
                "Send a case to urgent hearing", OTHER_DOCUMENT_MANUAL.getLabel(), idamTokens);
        assertNull(caseData.getSscsWelshPreviewNextEvent());

    }

    @Test
    public void shouldCallUpdateCaseWithUrgentCaseEvent() {
        SscsCaseData caseData = buildDataWithUrgentRequestDocument();
        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        when(ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                EventType.MAKE_CASE_URGENT.getCcdType(), "Send a case to urgent hearing",
                OTHER_DOCUMENT_MANUAL.getLabel(), idamTokens))
                .thenReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(ccdService).updateCase(caseData, callback.getCaseDetails().getId(), EventType.MAKE_CASE_URGENT.getCcdType(),
                "Send a case to urgent hearing", OTHER_DOCUMENT_MANUAL.getLabel(), idamTokens);
        assertNull(caseData.getSscsWelshPreviewNextEvent());

    }

    @Test
    public void shouldCallUpdateCaseWithUrgentCaseEventWhenWelshDocumentIsUrgentHaringRequest() {
        SscsWelshDocument sscsWelshDocument = SscsWelshDocument.builder().value(SscsWelshDocumentDetails.builder().documentType(DocumentType.URGENT_HEARING_REQUEST.getValue()).build()).build();
        List<SscsWelshDocument> sscsWelshDocuments = new ArrayList<>();
        sscsWelshDocuments.add(sscsWelshDocument);

        SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentType(DocumentType.OTHER_DOCUMENT.getValue()).build()).build();
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        sscsDocuments.add(sscsDocument);

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setSscsWelshDocuments(sscsWelshDocuments);
        caseData.setSscsDocument(sscsDocuments);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        when(ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                EventType.MAKE_CASE_URGENT.getCcdType(), "Send a case to urgent hearing",
                OTHER_DOCUMENT_MANUAL.getLabel(), idamTokens))
                .thenReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(ccdService).updateCase(caseData, callback.getCaseDetails().getId(), EventType.MAKE_CASE_URGENT.getCcdType(),
                "Send a case to urgent hearing", OTHER_DOCUMENT_MANUAL.getLabel(), idamTokens);
        assertNull(caseData.getSscsWelshPreviewNextEvent());
    }

    @Test
    public void shouldCallUpdateButNotCallUpdateUrgentCaseEventWhenUrgentCaseIsYes() {
        SscsCaseData caseData = buildDataWithUrgentRequestDocument();
        caseData.setUrgentCase("Yes");
        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
        when(ccdService.updateCase(caseData, callback.getCaseDetails().getId(), EventType.SEND_TO_DWP.getCcdType(),
                "Upload welsh document",
                "Upload welsh document", idamTokens))
                .thenReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);
        verify(ccdService).updateCase(caseData, callback.getCaseDetails().getId(), EventType.SEND_TO_DWP.getCcdType(),
                "Upload welsh document", "Upload welsh document", idamTokens);
        verify(ccdService, never()).updateCase(caseData, callback.getCaseDetails().getId(), EventType.MAKE_CASE_URGENT.getCcdType(),
                "Send a case to urgent hearing", OTHER_DOCUMENT_MANUAL.getLabel(), idamTokens);
        assertNull(caseData.getSscsWelshPreviewNextEvent());

    }

    @Test
    public void shouldSetReinstatementRequestWithWelshAndNonWelshReinstatementDocumentsWhenNonVoidOrDormant() {

        SscsWelshDocument sscsWelshDocument = SscsWelshDocument.builder().value(SscsWelshDocumentDetails.builder().documentType(DocumentType.REINSTATEMENT_REQUEST.getValue()).build()).build();

        List<SscsWelshDocument> sscsWelshDocuments = new ArrayList<>();
        sscsWelshDocuments.add(sscsWelshDocument);

        SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentType(DocumentType.REINSTATEMENT_REQUEST.getValue()).build()).build();
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        sscsDocuments.add(sscsDocument);

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setSscsWelshDocuments(sscsWelshDocuments);
        caseData.setSscsDocument(sscsDocuments);
        caseData.setPreviousState(State.APPEAL_CREATED);

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertNull(caseData.getSscsWelshPreviewNextEvent());
        assertEquals(RequestOutcome.IN_PROGRESS, caseData.getReinstatementOutcome());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, caseData.getInterlocReviewState());
        assertEquals(State.APPEAL_CREATED, caseData.getPreviousState());
    }

    @Test
    @Parameters({
        "DORMANT_APPEAL_STATE",
        "VOID_STATE",
    })
    public void shouldSetReinstatementRequestWithWelshAndNonWelshReinstatementDocumentsWhenVoidOrDormant(State state) {

        SscsWelshDocument sscsWelshDocument = SscsWelshDocument.builder().value(SscsWelshDocumentDetails.builder().documentType(DocumentType.REINSTATEMENT_REQUEST.getValue()).build()).build();

        List<SscsWelshDocument> sscsWelshDocuments = new ArrayList<>();
        sscsWelshDocuments.add(sscsWelshDocument);

        SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentType(DocumentType.REINSTATEMENT_REQUEST.getValue()).build()).build();
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        sscsDocuments.add(sscsDocument);

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setSscsWelshDocuments(sscsWelshDocuments);
        caseData.setSscsDocument(sscsDocuments);
        caseData.setPreviousState(state);

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertNull(caseData.getSscsWelshPreviewNextEvent());
        assertEquals(RequestOutcome.IN_PROGRESS, caseData.getReinstatementOutcome());
        assertEquals(State.INTERLOCUTORY_REVIEW_STATE, caseData.getPreviousState());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, caseData.getInterlocReviewState());
    }

    @Test
    public void shouldSetReinstatementRequestWithWelshButNoNonWelshReinstatementDocuments() {

        SscsWelshDocument sscsWelshDocument = SscsWelshDocument.builder().value(SscsWelshDocumentDetails.builder().documentType(DocumentType.REINSTATEMENT_REQUEST.getValue()).build()).build();

        List<SscsWelshDocument> sscsWelshDocuments = new ArrayList<>();
        sscsWelshDocuments.add(sscsWelshDocument);

        SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentType(DocumentType.OTHER_DOCUMENT.getValue()).build()).build();
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        sscsDocuments.add(sscsDocument);

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setSscsWelshDocuments(sscsWelshDocuments);
        caseData.setSscsDocument(sscsDocuments);
        caseData.setPreviousState(State.APPEAL_CREATED);
        caseData.setState(State.INTERLOCUTORY_REVIEW_STATE);

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertNull(caseData.getSscsWelshPreviewNextEvent());
        assertEquals(RequestOutcome.IN_PROGRESS, caseData.getReinstatementOutcome());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, caseData.getInterlocReviewState());
        assertEquals(State.APPEAL_CREATED, caseData.getPreviousState());
    }

    @Test
    public void shouldSetReinstatementRequestWithNoWelshButNonWelshReinstatementDocuments() {

        SscsWelshDocument sscsWelshDocument = SscsWelshDocument.builder().value(SscsWelshDocumentDetails.builder().documentType(DocumentType.OTHER_DOCUMENT.getValue()).build()).build();

        List<SscsWelshDocument> sscsWelshDocuments = new ArrayList<>();
        sscsWelshDocuments.add(sscsWelshDocument);

        SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentType(DocumentType.REINSTATEMENT_REQUEST.getValue()).build()).build();
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        sscsDocuments.add(sscsDocument);

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setSscsWelshDocuments(sscsWelshDocuments);
        caseData.setSscsDocument(sscsDocuments);
        caseData.setPreviousState(State.APPEAL_CREATED);
        caseData.setState(State.INTERLOCUTORY_REVIEW_STATE);

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertNull(caseData.getSscsWelshPreviewNextEvent());
        assertEquals(RequestOutcome.IN_PROGRESS, caseData.getReinstatementOutcome());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, caseData.getInterlocReviewState());
        assertEquals(State.APPEAL_CREATED, caseData.getPreviousState());
    }

    @Test
    public void shouldNotSetReinstatementRequestWithNoWelshAndNoNonWelshReinstatementDocuments() {

        SscsWelshDocument sscsWelshDocument = SscsWelshDocument.builder().value(SscsWelshDocumentDetails.builder().documentType(DocumentType.OTHER_DOCUMENT.getValue()).build()).build();

        List<SscsWelshDocument> sscsWelshDocuments = new ArrayList<>();
        sscsWelshDocuments.add(sscsWelshDocument);

        SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentType(DocumentType.OTHER_DOCUMENT.getValue()).build()).build();
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        sscsDocuments.add(sscsDocument);

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setSscsWelshDocuments(sscsWelshDocuments);
        caseData.setSscsDocument(sscsDocuments);
        caseData.setPreviousState(State.APPEAL_CREATED);
        caseData.setState(State.INTERLOCUTORY_REVIEW_STATE);

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        assertNull(caseData.getSscsWelshPreviewNextEvent());
        assertNull(caseData.getReinstatementOutcome());
        assertNull(caseData.getInterlocReviewState());
        assertEquals(State.APPEAL_CREATED, caseData.getPreviousState());
        assertEquals(State.INTERLOCUTORY_REVIEW_STATE, caseData.getState());
    }

    private Object[] generateCanHandleScenarios() {
        Callback<SscsCaseData> callbackWithValidEventOption = buildCallback(EventType.SEND_TO_DWP.getCcdType());
        return new Object[]{new Object[]{SUBMITTED, buildCallback("sendToDwp"), true},
            new Object[]{ABOUT_TO_SUBMIT, buildCallback(EventType.SEND_TO_DWP.getCcdType()), false},
            new Object[]{SUBMITTED, buildCallback(null), false},
            new Object[]{SUBMITTED, buildCallbackInterlocReviewState(), false}
        };
    }

    private Callback<SscsCaseData> buildCallback(String sscsWelshPreviewNextEvent) {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .sscsWelshPreviewNextEvent(sscsWelshPreviewNextEvent)
            .state(State.VALID_APPEAL)
            .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
            State.VALID_APPEAL, sscsCaseData, LocalDateTime.now(), "Benefit");
        return new Callback<>(caseDetails, Optional.empty(), UPLOAD_WELSH_DOCUMENT, false);
    }

    private Callback<SscsCaseData> buildCallbackInterlocReviewState() {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .state(State.INTERLOCUTORY_REVIEW_STATE)
            .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
            State.INTERLOCUTORY_REVIEW_STATE, sscsCaseData, LocalDateTime.now(), "Benefit");
        return new Callback<>(caseDetails, Optional.empty(), UPLOAD_WELSH_DOCUMENT, false);
    }

    private SscsCaseData buildDataWithUrgentRequestDocument() {
        SscsDocument sscsDocument = SscsDocument.builder().value(
                SscsDocumentDetails
                        .builder()
                        .documentType(DocumentType.URGENT_HEARING_REQUEST.getValue())
                        .documentDateAdded(LocalDateTime.now().toString())
                        .build())
                .build();
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        sscsDocuments.add(sscsDocument);
        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        sscsCaseData.setSscsDocument(sscsDocuments);
        return sscsCaseData;
    }
}
