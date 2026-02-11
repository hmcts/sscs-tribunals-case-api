package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.uploadwelshdocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPLOAD_WELSH_DOCUMENT;
import static uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.uploadwelshdocument.UploadWelshDocumentsSubmittedHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.RequestOutcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsWelshDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@ExtendWith(MockitoExtension.class)

public class UploadWelshDocumentsSubmittedHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private IdamService idamService;
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private UpdateCcdCaseService updateCcdCaseService;

    private SscsCaseData sscsCaseData;

    @Captor
    private ArgumentCaptor<Consumer<SscsCaseDetails>> consumerArgumentCaptor;

    @InjectMocks
    private UploadWelshDocumentsSubmittedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UploadWelshDocumentsSubmittedHandler(
                idamService, updateCcdCaseService);
        lenient().when(idamService.getIdamTokens()).thenReturn(IdamTokens.builder().build());
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build())
                .sscsWelshPreviewNextEvent("sendToDwp")
                .build();
    }

    @ParameterizedTest
    @MethodSource("generateCanHandleScenarios")
    void givenCanHandleIsCalled_shouldReturnCorrectResult(CallbackType callbackType,
                                                          Callback<SscsCaseData> callback,
                                                          boolean expectedResult) {
        boolean actualResult = handler.canHandle(callbackType, callback);
        assertEquals(expectedResult, actualResult);
    }

    @Test
    void shouldCallUpdateCaseWithCorrectEvent() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        when(updateCcdCaseService.updateCaseV2(anyLong(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(SscsCaseDetails.builder().data(caseData).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verifyEventTrigger(EventType.SEND_TO_DWP, "Upload Welsh document", "Upload Welsh document", caseData);
    }

    @Test
    void shouldCallUpdateCaseWithUrgentCaseEvent() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        SscsCaseData caseData = buildDataWithUrgentRequestDocument();

        when(updateCcdCaseService.updateCaseV2(anyLong(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(SscsCaseDetails.builder().data(caseData).build());
        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verifyEventTrigger(EventType.MAKE_CASE_URGENT, "Send a case to urgent hearing", OTHER_DOCUMENT_MANUAL.getLabel(), caseData);
    }

    @Test
    void shouldCallUpdateCaseWithUrgentCaseEventWhenWelshDocumentIsUrgentHaringRequest() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        SscsWelshDocument sscsWelshDocument = SscsWelshDocument.builder().value(SscsWelshDocumentDetails.builder().documentType(DocumentType.URGENT_HEARING_REQUEST.getValue()).build()).build();
        List<SscsWelshDocument> sscsWelshDocuments = new ArrayList<>();
        sscsWelshDocuments.add(sscsWelshDocument);

        SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentType(DocumentType.OTHER_DOCUMENT.getValue()).build()).build();
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        sscsDocuments.add(sscsDocument);

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setSscsWelshDocuments(sscsWelshDocuments);
        caseData.setSscsDocument(sscsDocuments);

        when(updateCcdCaseService.updateCaseV2(anyLong(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(SscsCaseDetails.builder().data(caseData).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verifyEventTrigger(EventType.MAKE_CASE_URGENT, "Send a case to urgent hearing", OTHER_DOCUMENT_MANUAL.getLabel(), caseData);
    }

    @Test
    void shouldCallUpdateButNotCallUpdateUrgentCaseEventWhenUrgentCaseIsYes() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        SscsCaseData caseData = buildDataWithUrgentRequestDocument();
        caseData.setUrgentCase("Yes");

        when(updateCcdCaseService.updateCaseV2(anyLong(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(SscsCaseDetails.builder().data(caseData).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verifyEventTrigger(EventType.SEND_TO_DWP, "Upload Welsh document", "Upload Welsh document", caseData);
    }

    @Test
    void shouldSetReinstatementRequestWithWelshAndNonWelshReinstatementDocumentsWhenNonVoidOrDormant() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

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

        when(updateCcdCaseService.updateCaseV2(anyLong(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(SscsCaseDetails.builder().data(caseData).build());
        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verifyEventTrigger(EventType.SEND_TO_DWP, "Upload Welsh document", "Upload Welsh document", caseData);
        assertEquals(RequestOutcome.IN_PROGRESS, caseData.getReinstatementOutcome());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, caseData.getInterlocReviewState());
        assertEquals(State.APPEAL_CREATED, caseData.getPreviousState());
    }

    @ParameterizedTest
    @EnumSource(value = State.class, names = {"DORMANT_APPEAL_STATE", "VOID_STATE"})
    void shouldSetReinstatementRequestWithWelshAndNonWelshReinstatementDocumentsWhenVoidOrDormant(State state) {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

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

        when(updateCcdCaseService.updateCaseV2(anyLong(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(SscsCaseDetails.builder().data(caseData).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verifyEventTrigger(EventType.SEND_TO_DWP, "Upload Welsh document", "Upload Welsh document", caseData);
        assertEquals(RequestOutcome.IN_PROGRESS, caseData.getReinstatementOutcome());
        assertEquals(State.INTERLOCUTORY_REVIEW_STATE, caseData.getPreviousState());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, caseData.getInterlocReviewState());
    }


    @Test
    void shouldSetReinstatementRequestWithWelshButNoNonWelshReinstatementDocuments() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

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

        when(updateCcdCaseService.updateCaseV2(anyLong(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(SscsCaseDetails.builder().data(caseData).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verifyEventTrigger(EventType.SEND_TO_DWP, "Upload Welsh document", "Upload Welsh document", caseData);
        assertEquals(RequestOutcome.IN_PROGRESS, caseData.getReinstatementOutcome());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, caseData.getInterlocReviewState());
        assertEquals(State.APPEAL_CREATED, caseData.getPreviousState());
    }

    @Test
    void shouldSetReinstatementRequestWithNoWelshButNonWelshReinstatementDocuments() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

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

        when(updateCcdCaseService.updateCaseV2(anyLong(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(SscsCaseDetails.builder().data(caseData).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verifyEventTrigger(EventType.SEND_TO_DWP, "Upload Welsh document", "Upload Welsh document", caseData);
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(caseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertNull(caseData.getSscsWelshPreviewNextEvent());
        assertEquals(RequestOutcome.IN_PROGRESS, caseData.getReinstatementOutcome());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, caseData.getInterlocReviewState());
        assertEquals(State.APPEAL_CREATED, caseData.getPreviousState());
    }

    @Test
    void shouldNotSetReinstatementRequestWithNoWelshAndNoNonWelshReinstatementDocuments() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

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

        when(updateCcdCaseService.updateCaseV2(anyLong(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(SscsCaseDetails.builder().data(caseData).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verifyEventTrigger(EventType.SEND_TO_DWP, "Upload Welsh document", "Upload Welsh document", caseData);
        assertNull(caseData.getReinstatementOutcome());
        assertNull(caseData.getInterlocReviewState());
        assertEquals(State.APPEAL_CREATED, caseData.getPreviousState());
        assertEquals(State.INTERLOCUTORY_REVIEW_STATE, caseData.getState());
    }

    @Test
    void shouldUpdateCaseWhenNextEventIsNotEmpty() {
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        sscsCaseData.setSscsWelshPreviewNextEvent("sendToDwp");

        when(updateCcdCaseService.updateCaseV2(anyLong(), anyString(), anyString(), anyString(), any(), any()))
                .thenReturn(SscsCaseDetails.builder().data(sscsCaseData).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(updateCcdCaseService).updateCaseV2(eq(callback.getCaseDetails().getId()), eq("sendToDwp"),
                eq("Upload Welsh document"), eq("Upload Welsh document"), any(), any());
    }

    private void verifyEventTrigger(EventType makeCaseUrgent, String summary, String description, SscsCaseData caseData) {
        verify(updateCcdCaseService).updateCaseV2(eq(callback.getCaseDetails().getId()), eq(makeCaseUrgent.getCcdType()),
                eq(summary), eq(description), isA(IdamTokens.class), consumerArgumentCaptor.capture());
        SscsCaseDetails sscsCaseDetails = SscsCaseDetails.builder().data(caseData).build();
        consumerArgumentCaptor.getValue().accept(sscsCaseDetails);
        assertNull(caseData.getSscsWelshPreviewNextEvent());
    }

    private static Stream<Arguments> generateCanHandleScenarios() {
        return Stream.of(
                Arguments.of(SUBMITTED, buildCallback("sendToDwp"), true),
                Arguments.of(ABOUT_TO_SUBMIT, buildCallback(EventType.SEND_TO_DWP.getCcdType()), false),
                Arguments.of(SUBMITTED, buildCallback(null), false),
                Arguments.of(SUBMITTED, buildCallbackInterlocReviewState(), false)
        );
    }

    private static Callback<SscsCaseData> buildCallback(String sscsWelshPreviewNextEvent) {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .sscsWelshPreviewNextEvent(sscsWelshPreviewNextEvent)
                .state(State.VALID_APPEAL)
                .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
                State.VALID_APPEAL, sscsCaseData, LocalDateTime.now(), "Benefit");
        return new Callback<>(caseDetails, Optional.empty(), UPLOAD_WELSH_DOCUMENT, false);
    }

    private static Callback<SscsCaseData> buildCallbackInterlocReviewState() {
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
