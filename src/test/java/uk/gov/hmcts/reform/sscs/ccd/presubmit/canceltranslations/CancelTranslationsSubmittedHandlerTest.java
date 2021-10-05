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
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(JUnitParamsRunner.class)
public class CancelTranslationsSubmittedHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private CancelTranslationsSubmittedHandler handler;

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
        handler = new CancelTranslationsSubmittedHandler(ccdService, idamService);
        when(callback.getEvent()).thenReturn(EventType.CANCEL_TRANSLATIONS);
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
            "Cancel welsh translations",
            "Cancel welsh translations", idamTokens))
            .thenReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);
        verify(ccdService).updateCase(caseData, callback.getCaseDetails().getId(), EventType.SEND_TO_DWP.getCcdType(),
            "Cancel welsh translations", "Cancel welsh translations", idamTokens);
        verify(ccdService, never()).updateCase(caseData, callback.getCaseDetails().getId(), EventType.MAKE_CASE_URGENT.getCcdType(),
                "Send a case to urgent hearing", OTHER_DOCUMENT_MANUAL.getLabel(), idamTokens);
        assertNull(caseData.getSscsWelshPreviewNextEvent());

    }

    @Test
    public void shouldCallUpdateCaseWithUrgentCaseEvent() {
        SscsCaseData caseData = buildDataWithDocumentType(DocumentType.URGENT_HEARING_REQUEST.getValue());
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
        SscsCaseData caseData = buildDataWithDocumentType(DocumentType.URGENT_HEARING_REQUEST.getValue());
        caseData.setUrgentCase("Yes");
        IdamTokens idamTokens = IdamTokens.builder().build();
        when(idamService.getIdamTokens()).thenReturn(idamTokens);
        when(ccdService.updateCase(caseData, callback.getCaseDetails().getId(), EventType.SEND_TO_DWP.getCcdType(),
                "Cancel welsh translations",
                "Cancel welsh translations", idamTokens))
                .thenReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);
        verify(ccdService).updateCase(caseData, callback.getCaseDetails().getId(), EventType.SEND_TO_DWP.getCcdType(),
                "Cancel welsh translations", "Cancel welsh translations", idamTokens);
        verify(ccdService, never()).updateCase(caseData, callback.getCaseDetails().getId(), EventType.MAKE_CASE_URGENT.getCcdType(),
                "Send a case to urgent hearing", OTHER_DOCUMENT_MANUAL.getLabel(), idamTokens);
        assertNull(caseData.getSscsWelshPreviewNextEvent());
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

        when(ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                EventType.UPDATE_CASE_ONLY.getCcdType(), "Set Reinstatement Request",
                "Set Reinstatement Request", idamTokens))
                .thenReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(ccdService).updateCase(caseData, callback.getCaseDetails().getId(), EventType.UPDATE_CASE_ONLY.getCcdType(),
                "Set Reinstatement Request", "Set Reinstatement Request", idamTokens);
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

        when(ccdService.updateCase(caseData, callback.getCaseDetails().getId(),
                EventType.UPDATE_CASE_ONLY.getCcdType(), "Cancel welsh translations",
                "Cancel welsh translations", idamTokens))
                .thenReturn(SscsCaseDetails.builder().data(SscsCaseData.builder().build()).build());

        handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(ccdService).updateCase(caseData, callback.getCaseDetails().getId(), EventType.UPDATE_CASE_ONLY.getCcdType(),
                "Cancel welsh translations", "Cancel welsh translations", idamTokens);
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
