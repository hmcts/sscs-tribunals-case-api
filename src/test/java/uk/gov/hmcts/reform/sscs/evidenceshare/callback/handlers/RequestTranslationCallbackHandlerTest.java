package uk.gov.hmcts.reform.sscs.evidenceshare.callback.handlers;

import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.REQUEST_TRANSLATION_FROM_WLU;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.APPEAL_CREATED;

import java.time.LocalDateTime;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.evidenceshare.exception.WelshException;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.RequestTranslationService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;

@RunWith(JUnitParamsRunner.class)
public class RequestTranslationCallbackHandlerTest {
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CcdService ccdCaseService;

    @Mock
    private IdamService idamService;

    @Mock
    private RequestTranslationService requestTranslationService;

    private RequestTranslationCallbackHandler handler;

    @Captor
    ArgumentCaptor<SscsCaseData> captor;

    @Before
    public void setUp() {
        handler = new RequestTranslationCallbackHandler(requestTranslationService, ccdCaseService, idamService);

    }

    @Test(expected = IllegalStateException.class)
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "ABOUT_TO_SUBMIT"})
    public void givenCallbackIsNotSubmitted_willThrowAnException(CallbackType callbackType) {
        handler.handle(callbackType,
            HandlerHelper.buildTestCallbackForGivenData(SscsCaseData.builder().languagePreferenceWelsh("No").build(), APPEAL_CREATED, REQUEST_TRANSLATION_FROM_WLU));
    }

    @Test
    public void givenCallbackIsSubmitted_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(REQUEST_TRANSLATION_FROM_WLU);
        when(callback.getCaseDetails()).thenReturn(getCaseDetails("Yes"));
        assertTrue(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void requestTranslationForNonWelshCase() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("No");
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), REQUEST_TRANSLATION_FROM_WLU, false);

        Exception exception = assertThrows(IllegalStateException.class, () ->
            handler.handle(SUBMITTED, callback));

        assertEquals("Error: This action is only available for Welsh cases", exception.getMessage());
    }

    @Test
    public void requestTranslationForWelshCase() {
        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("Yes");
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), REQUEST_TRANSLATION_FROM_WLU, false);
        when(requestTranslationService.sendCaseToWlu(any())).thenReturn(true);
        handler.handle(SUBMITTED, callback);

        verify(requestTranslationService).sendCaseToWlu(any());
        verify(ccdCaseService).updateCase(captor.capture(), eq(123L), eq(EventType.CASE_UPDATED.getCcdType()), eq("Case translations sent to wlu"), eq("Updated case with date sent to wlu"), any());
    }

    @Test
    public void whenCallbackFailsThrowWelshException() {
        RequestTranslationCallbackHandler mockHandle = mock(RequestTranslationCallbackHandler.class);
        Mockito.doThrow(new WelshException(new Exception())).when(mockHandle).handle(any(), any());

        CaseDetails<SscsCaseData> caseDetails = getCaseDetails("Yes");
        Callback<SscsCaseData> callback = new Callback<>(caseDetails, Optional.empty(), REQUEST_TRANSLATION_FROM_WLU, false);

        WelshException exception = assertThrows(
            WelshException.class, () -> mockHandle.handle(SUBMITTED, callback));
        assertNotNull(exception.getMessage());
    }

    private CaseDetails<SscsCaseData> getCaseDetails(String languagePreference) {
        SscsCaseData caseData = SscsCaseData.builder()
            .ccdCaseId("123")
            .appeal(Appeal.builder().build())
            .languagePreferenceWelsh(languagePreference)
            .build();

        return new CaseDetails<>(123L, "jurisdiction", APPEAL_CREATED, caseData, LocalDateTime.now(), "Benefit");
    }

}
