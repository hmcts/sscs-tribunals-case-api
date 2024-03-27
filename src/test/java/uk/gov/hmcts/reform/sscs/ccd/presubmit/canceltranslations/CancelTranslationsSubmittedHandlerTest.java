package uk.gov.hmcts.reform.sscs.ccd.presubmit.canceltranslations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CANCEL_TRANSLATIONS;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.furtherevidence.actionfurtherevidence.FurtherEvidenceActionDynamicListItems.OTHER_DOCUMENT_MANUAL;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import uk.gov.hmcts.reform.ccd.client.model.StartEventResponse;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.SscsCcdConvertService;
import uk.gov.hmcts.reform.sscs.ccd.service.UpdateCcdCaseService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(JUnitParamsRunner.class)
public class CancelTranslationsSubmittedHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private CancelTranslationsSubmittedHandler handler;

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

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        handler = new CancelTranslationsSubmittedHandler(
                idamService, ccdClient, sscsCcdConvertService, updateCcdCaseService);

        when(callback.getEvent()).thenReturn(EventType.CANCEL_TRANSLATIONS);
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().build())
            .sscsWelshPreviewNextEvent("sendToDwp")
            .build();
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(ccdClient.startEvent(any(IdamTokens.class), anyLong(), eq(EventType.UPDATE_CASE_ONLY.getCcdType())))
                .thenReturn(startEventResponse);
        when(startEventResponse.getCaseDetails()).thenReturn(ccdCaseDetails);
        when(ccdCaseDetails.getData()).thenReturn(Map.of());
        when(sscsCcdConvertService.getCaseData(anyMap())).thenReturn(sscsCaseData);

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
        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(updateCcdCaseService).updateCaseV2(eq(callback.getCaseDetails().getId()), eq(EventType.SEND_TO_DWP.getCcdType()),
                eq("Cancel welsh translations"), eq("Cancel welsh translations"), eq(idamTokens), sscsCaseDataCaptor.capture());

        SscsCaseData caseData = callback.getCaseDetails().getCaseData();
        Consumer<SscsCaseData> caseDataCaptorValue = sscsCaseDataCaptor.getValue();
        caseDataCaptorValue.accept(caseData);
        assertNull(caseData.getSscsWelshPreviewNextEvent());

        verify(updateCcdCaseService, never()).updateCaseV2(eq(callback.getCaseDetails().getId()), eq(EventType.MAKE_CASE_URGENT.getCcdType()),
                eq("Send a case to urgent hearing"), eq(OTHER_DOCUMENT_MANUAL.getLabel()), any(IdamTokens.class), any(Consumer.class));
    }

    @Test
    public void shouldCallUpdateCaseWithUrgentCaseEvent() {
        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);

        SscsCaseData caseData = buildDataWithDocumentType(DocumentType.URGENT_HEARING_REQUEST.getValue());
        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(updateCcdCaseService).updateCaseV2(eq(callback.getCaseDetails().getId()), eq(EventType.MAKE_CASE_URGENT.getCcdType()),
                eq("Send a case to urgent hearing"), eq(OTHER_DOCUMENT_MANUAL.getLabel()), eq(idamTokens), sscsCaseDataCaptor.capture());

        Consumer<SscsCaseData> caseDataCaptorValue = sscsCaseDataCaptor.getValue();
        caseDataCaptorValue.accept(caseData);
        assertNull(caseData.getSscsWelshPreviewNextEvent());
    }

    @Test
    public void shouldCallUpdateButNotCallUpdateUrgentCaseEventWhenUrgentCaseIsYes() {
        SscsCaseData caseData = buildDataWithDocumentType(DocumentType.URGENT_HEARING_REQUEST.getValue());
        caseData.setUrgentCase("Yes");
        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
        /*when(updateCcdCaseService.updateCaseV2(callback.getCaseDetails().getId(), EventType.SEND_TO_DWP.getCcdType(),
                "Cancel welsh translations",
                "Cancel welsh translations", idamTokens, any(Consumer.class)))
                .thenReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());*/

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(updateCcdCaseService).updateCaseV2(eq(callback.getCaseDetails().getId()), eq(EventType.SEND_TO_DWP.getCcdType()),
                eq("Cancel welsh translations"), eq("Cancel welsh translations"), eq(idamTokens), sscsCaseDataCaptor.capture());
        Consumer<SscsCaseData> caseDataCaptorValue = sscsCaseDataCaptor.getValue();
        caseDataCaptorValue.accept(caseData);
        assertNull(caseData.getSscsWelshPreviewNextEvent());

        verify(updateCcdCaseService, never()).updateCaseV2(eq(callback.getCaseDetails().getId()), eq(EventType.MAKE_CASE_URGENT.getCcdType()),
                eq("Send a case to urgent hearing"), eq(OTHER_DOCUMENT_MANUAL.getLabel()), eq(idamTokens), any(Consumer.class));
    }

    @Test
    public void shouldCallUpdateCaseWithReinstatementRequest() {
        SscsCaseData caseData = buildDataWithDocumentType(DocumentType.DIRECTION_NOTICE.getValue());
        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
        caseData.setSscsWelshPreviewNextEvent(EventType.UPDATE_CASE_ONLY.getCcdType());

        SscsDocument sscsDocument = SscsDocument.builder().value(
                SscsDocumentDetails
                        .builder()
                        .documentType(DocumentType.REINSTATEMENT_REQUEST.getValue())
                        .documentDateAdded(LocalDateTime.now().toString())
                        .build())
                .build();

        caseData.getSscsDocument().add(sscsDocument);

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(updateCcdCaseService).updateCaseV2(eq(callback.getCaseDetails().getId()), eq(EventType.UPDATE_CASE_ONLY.getCcdType()),
                eq("Set Reinstatement Request"), eq("Set Reinstatement Request"), eq(idamTokens), sscsCaseDataCaptor.capture());
        Consumer<SscsCaseData> caseDataCaptorValue = sscsCaseDataCaptor.getValue();
        caseDataCaptorValue.accept(caseData);
        assertNull(caseData.getSscsWelshPreviewNextEvent());
    }

    @Test
    public void shouldNotCallUpdateCaseWithReinstatementRequestWhenReinstatementRequestIsAlreadySet() {
        SscsCaseData caseData = buildDataWithDocumentType(DocumentType.REINSTATEMENT_REQUEST.getValue());

        SscsDocument sscsDocument = SscsDocument.builder().value(
                SscsDocumentDetails
                        .builder()
                        .documentType(DocumentType.DIRECTION_NOTICE.getValue())
                        .documentDateAdded(LocalDateTime.now().toString())
                        .build())
                .build();

        caseData.getSscsDocument().add(sscsDocument);
        caseData.setReinstatementOutcome(RequestOutcome.GRANTED);

        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
        caseData.setSscsWelshPreviewNextEvent(EventType.UPDATE_CASE_ONLY.getCcdType());

        /*when(ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                EventType.UPDATE_CASE_ONLY.getCcdType(), "Cancel welsh translations",
                "Cancel welsh translations", idamTokens))
                .thenReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());*/

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(updateCcdCaseService).updateCaseV2(eq(callback.getCaseDetails().getId()), eq(EventType.UPDATE_CASE_ONLY.getCcdType()),
                eq("Cancel welsh translations"), eq("Cancel welsh translations"), eq(idamTokens), sscsCaseDataCaptor.capture());
        Consumer<SscsCaseData> caseDataCaptorValue = sscsCaseDataCaptor.getValue();
        caseDataCaptorValue.accept(caseData);
        assertNull(caseData.getSscsWelshPreviewNextEvent());
    }

    private Object[] generateCanHandleScenarios() {
        Callback<SscsCaseData> callbackWithValidEventOption = buildCallback(EventType.SEND_TO_DWP.getCcdType(), State.VALID_APPEAL);
        return new Object[]{new Object[]{SUBMITTED, buildCallback("sendToDwp", State.VALID_APPEAL), true},
            new Object[]{ABOUT_TO_SUBMIT, buildCallback(EventType.SEND_TO_DWP.getCcdType(), State.VALID_APPEAL), false},
            new Object[]{SUBMITTED, buildCallback(null, State.VALID_APPEAL), false},
            new Object[]{SUBMITTED, buildCallback(null, State.INTERLOCUTORY_REVIEW_STATE), false}
        };
    }

    private Callback<SscsCaseData> buildCallback(String sscsWelshPreviewNextEvent, State state) {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .sscsWelshPreviewNextEvent(sscsWelshPreviewNextEvent)
            .state(state)
            .build();
        CaseDetails<SscsCaseData> caseDetails = new CaseDetails<>(123L, "sscs",
            state, sscsCaseData, LocalDateTime.now(), "Benefit");
        return new Callback<>(caseDetails, Optional.empty(), CANCEL_TRANSLATIONS, false);
    }

    private SscsCaseData buildDataWithDocumentType(String documentType) {
        SscsDocument sscsDocument = SscsDocument.builder().value(
                SscsDocumentDetails
                        .builder()
                        .documentType(documentType)
                        .documentDateAdded(LocalDateTime.now().toString())
                        .build()).build();
        List<SscsDocument> sscsDocuments = new ArrayList<>();
        sscsDocuments.add(sscsDocument);
        SscsCaseData sscsCaseData = callback.getCaseDetails().getCaseData();
        sscsCaseData.setSscsDocument(sscsDocuments);
        return sscsCaseData;
    }

}
