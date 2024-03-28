package uk.gov.hmcts.reform.sscs.ccd.presubmit.uploadwelshdocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPLOAD_WELSH_DOCUMENT;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    @Mock
    private CcdClient ccdClient;
    @Mock
    private SscsCcdConvertService sscsCcdConvertService;
    @Mock
    private uk.gov.hmcts.reform.ccd.client.model.CaseDetails ccdCaseDetails;
    @Mock
    private StartEventResponse startEventResponse;

    private SscsCaseData sscsCaseData;

    @Captor
    private ArgumentCaptor<Consumer<SscsCaseData>> sscsCaseDataCaptor;

    @InjectMocks
    private UploadWelshDocumentsSubmittedHandler handler;

    @BeforeEach
    void setUp() {
        handler = new UploadWelshDocumentsSubmittedHandler(
                idamService, ccdClient, sscsCcdConvertService, updateCcdCaseService);
        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        when(callback.getEvent()).thenReturn(EventType.UPLOAD_WELSH_DOCUMENT);
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build())
                .sscsWelshPreviewNextEvent("sendToDwp")
                .build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        when(startEventResponse.getCaseDetails()).thenReturn(ccdCaseDetails);
        when(startEventResponse.getCaseDetails().getData()).thenReturn(Map.of("sscsWelshPreviewNextEvent", "sendToDwp"));
        when(ccdCaseDetails.getData()).thenReturn(Map.of("sscsWelshPreviewNextEvent", "sendToDwp"));
        when(sscsCcdConvertService.getCaseData(anyMap())).thenReturn(sscsCaseData);
        when(ccdClient.startEvent(any(), anyLong(), eq(EventType.UPDATE_CASE_ONLY.getCcdType())))
                .thenReturn(startEventResponse);
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
        SscsCaseData caseData = callback.getCaseDetails().getCaseData();

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verifyEventTrigger(EventType.SEND_TO_DWP, "Upload welsh document", "Upload welsh document", caseData);
    }

    @Test
    void shouldCallUpdateCaseWithUrgentCaseEvent() {
        SscsCaseData caseData = buildDataWithUrgentRequestDocument();

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verifyEventTrigger(EventType.MAKE_CASE_URGENT, "Send a case to urgent hearing", OTHER_DOCUMENT_MANUAL.getLabel(), caseData);
    }

    @Test
    void shouldCallUpdateCaseWithUrgentCaseEventWhenWelshDocumentIsUrgentHaringRequest() {
        SscsWelshDocument sscsWelshDocument = SscsWelshDocument.builder().value(SscsWelshDocumentDetails.builder().documentType(DocumentType.URGENT_HEARING_REQUEST.getValue()).build()).build();
        List<SscsWelshDocument> sscsWelshDocuments = new ArrayList<>();
        sscsWelshDocuments.add(sscsWelshDocument);

        SscsDocument sscsDocument = SscsDocument.builder().value(SscsDocumentDetails.builder().documentType(DocumentType.OTHER_DOCUMENT.getValue()).build()).build();
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        sscsDocuments.add(sscsDocument);

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        caseData.setSscsWelshDocuments(sscsWelshDocuments);
        caseData.setSscsDocument(sscsDocuments);

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verifyEventTrigger(EventType.MAKE_CASE_URGENT, "Send a case to urgent hearing", OTHER_DOCUMENT_MANUAL.getLabel(), caseData);
    }

    @Test
    void shouldCallUpdateButNotCallUpdateUrgentCaseEventWhenUrgentCaseIsYes() {
        SscsCaseData caseData = buildDataWithUrgentRequestDocument();
        caseData.setUrgentCase("Yes");

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verifyEventTrigger(EventType.SEND_TO_DWP, "Upload welsh document", "Upload welsh document", caseData);
    }

    @Test
    void shouldSetReinstatementRequestWithWelshAndNonWelshReinstatementDocumentsWhenNonVoidOrDormant() {
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

        verifyEventTrigger(EventType.SEND_TO_DWP, "Upload Welsh Document", "Upload Welsh Document", caseData);
        assertEquals(RequestOutcome.IN_PROGRESS, caseData.getReinstatementOutcome());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, caseData.getInterlocReviewState());
        assertEquals(State.APPEAL_CREATED, caseData.getPreviousState());
    }

    @ParameterizedTest
    @EnumSource(value = State.class, names = {"DORMANT_APPEAL_STATE", "VOID_STATE"})
    void shouldSetReinstatementRequestWithWelshAndNonWelshReinstatementDocumentsWhenVoidOrDormant(State state) {
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

        verifyEventTrigger(EventType.SEND_TO_DWP, "Upload Welsh Document", "Upload Welsh Document", caseData);
        assertEquals(RequestOutcome.IN_PROGRESS, caseData.getReinstatementOutcome());
        assertEquals(State.INTERLOCUTORY_REVIEW_STATE, caseData.getPreviousState());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, caseData.getInterlocReviewState());
    }

    @Test
    void shouldSetReinstatementRequestWithWelshButNoNonWelshReinstatementDocuments() {
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

        verifyEventTrigger(EventType.SEND_TO_DWP, "Upload Welsh Document", "Upload Welsh Document", caseData);
        assertEquals(RequestOutcome.IN_PROGRESS, caseData.getReinstatementOutcome());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, caseData.getInterlocReviewState());
        assertEquals(State.APPEAL_CREATED, caseData.getPreviousState());
    }

    @Test
    void shouldSetReinstatementRequestWithNoWelshButNonWelshReinstatementDocuments() {

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

        verifyEventTrigger(EventType.SEND_TO_DWP, "Upload Welsh Document", "Upload Welsh Document", caseData);
        assertEquals(RequestOutcome.IN_PROGRESS, caseData.getReinstatementOutcome());
        assertEquals(InterlocReviewState.REVIEW_BY_JUDGE, caseData.getInterlocReviewState());
        assertEquals(State.APPEAL_CREATED, caseData.getPreviousState());
    }

    @Test
    void shouldNotSetReinstatementRequestWithNoWelshAndNoNonWelshReinstatementDocuments() {

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

        verifyEventTrigger(EventType.SEND_TO_DWP, "Upload welsh document", "Upload welsh document", caseData);
        assertNull(caseData.getReinstatementOutcome());
        assertNull(caseData.getInterlocReviewState());
        assertEquals(State.APPEAL_CREATED, caseData.getPreviousState());
        assertEquals(State.INTERLOCUTORY_REVIEW_STATE, caseData.getState());
    }

    private void verifyEventTrigger(EventType makeCaseUrgent, String summary, String description, SscsCaseData caseData) {
        verify(updateCcdCaseService).updateCaseV2(eq(callback.getCaseDetails().getId()), eq(makeCaseUrgent.getCcdType()),
                eq(summary), eq(description), any(), sscsCaseDataCaptor.capture());
        Consumer<SscsCaseData> consumerCaptorValue = sscsCaseDataCaptor.getValue();
        consumerCaptorValue.accept(caseData);
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
