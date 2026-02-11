package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit;

import static java.time.LocalDateTime.now;
import static java.util.Optional.empty;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.DWP_UPLOAD_RESPONSE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL_CREATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.READY_TO_LIST;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.callback.PreSubmitCallbackDispatcher;
import uk.gov.hmcts.reform.sscs.callback.handlers.PreSubmitCallbackHandler;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.exception.RequiredFieldMissingException;

@ExtendWith(MockitoExtension.class)
public class PreSubmitCallbackDispatcherTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock
    private PreSubmitCallbackHandler<CaseData> handler1;
    @Mock
    private PreSubmitCallbackHandler<CaseData> handler2;
    private Callback<CaseData> callback;
    private CaseData caseData;
    private CaseData caseDataMutation1;
    private CaseData caseDataMutation2;
    private PreSubmitCallbackResponse<CaseData> response1;
    private PreSubmitCallbackResponse<CaseData> response2;

    private PreSubmitCallbackDispatcher<CaseData> preSubmitCallbackDispatcher;

    @BeforeEach
    public void setUp() {
        caseData = buildCaseData();
        caseDataMutation1 = buildCaseData();
        caseDataMutation2 = buildCaseData();
        CaseDetails<CaseData> caseDetails =
                new CaseDetails<>(1234L, "SSCS", READY_TO_LIST, caseData, now(), "Benefit");
        callback = new Callback<>(caseDetails, empty(), DWP_UPLOAD_RESPONSE, false);
        callback.setPageId("pageId");
        response1 = new PreSubmitCallbackResponse<>(caseDataMutation1);
        response1.addErrors(List.of("error1"));
        response2 = new PreSubmitCallbackResponse<>(caseDataMutation2);
        response2.addErrors(List.of("error2", "error3"));
        preSubmitCallbackDispatcher = new PreSubmitCallbackDispatcher<>(List.of(handler1, handler2));
    }

    @Test
    public void should_dispatch_callback_to_handlers_collecting_any_error_messages() {
        when(handler1.canHandle(eq(ABOUT_TO_SUBMIT), eq(callback))).thenReturn(true);
        when(handler1.handle(eq(ABOUT_TO_SUBMIT), any(Callback.class), eq(USER_AUTHORISATION))).thenReturn(response1);

        when(handler2.canHandle(eq(ABOUT_TO_SUBMIT), any(Callback.class))).thenReturn(true);
        when(handler2.handle(eq(ABOUT_TO_SUBMIT), any(Callback.class), eq(USER_AUTHORISATION))).thenReturn(response2);

        PreSubmitCallbackResponse<CaseData> callbackResponse =
                preSubmitCallbackDispatcher.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNotNull(callbackResponse);
        assertEquals(caseDataMutation2, callbackResponse.getData());
        assertThat(callbackResponse.getErrors()).containsAll(List.of("error1", "error2", "error3"));

        ArgumentCaptor<Callback> callbackCaptor = ArgumentCaptor.forClass(Callback.class);
        verify(handler1).handle(eq(ABOUT_TO_SUBMIT), callbackCaptor.capture(), eq(USER_AUTHORISATION));
        assertEquals(caseData, callbackCaptor.getValue().getCaseDetails().getCaseData());
        verify(handler2).handle(eq(ABOUT_TO_SUBMIT), callbackCaptor.capture(), eq(USER_AUTHORISATION));
        assertEquals(caseDataMutation1, callbackCaptor.getValue().getCaseDetails().getCaseData());
        assertEquals("pageId", callbackCaptor.getValue().getPageId());
    }

    @Test
    public void should_not_throw_exception_for_create_case_start_callbacks() {
        CaseDetails<CaseData> caseDetails =
                new CaseDetails<>(1234L, "SSCS", null, caseData, null, "Benefit");
        callback = new Callback<>(caseDetails, empty(), VALID_APPEAL_CREATED, false);
        when(handler1.canHandle(eq(ABOUT_TO_START), eq(callback))).thenReturn(true);
        when(handler1.handle(eq(ABOUT_TO_START), any(Callback.class), eq(USER_AUTHORISATION))).thenReturn(response1);

        assertDoesNotThrow(() -> preSubmitCallbackDispatcher.handle(ABOUT_TO_START, callback, USER_AUTHORISATION));
    }

    @Test
    public void should_throw_exception_if_state_not_set_for_non_create_case_start_callbacks() {
        CaseDetails<CaseData> caseDetails =
                new CaseDetails<>(1234L, "SSCS", null, caseData, null, "Benefit");
        callback = new Callback<>(caseDetails, empty(), VALID_APPEAL_CREATED, false);
        when(handler1.canHandle(eq(ABOUT_TO_SUBMIT), eq(callback))).thenReturn(true);

        assertThrows(RequiredFieldMissingException.class,
                () -> preSubmitCallbackDispatcher.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }

    @Test
    public void should_only_dispatch_callback_to_handlers_that_can_handle_it() {
        when(handler1.canHandle(eq(ABOUT_TO_SUBMIT), any(Callback.class))).thenReturn(false);
        when(handler2.canHandle(eq(ABOUT_TO_SUBMIT), any(Callback.class))).thenReturn(false);

        PreSubmitCallbackResponse<CaseData> callbackResponse =
                preSubmitCallbackDispatcher.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNotNull(callbackResponse);
        assertEquals(caseData, callbackResponse.getData());
        assertTrue(callbackResponse.getErrors().isEmpty());

        verify(handler1, never()).handle(eq(ABOUT_TO_SUBMIT), any(Callback.class), eq(USER_AUTHORISATION));
        verify(handler2, never()).handle(eq(ABOUT_TO_SUBMIT), any(Callback.class), eq(USER_AUTHORISATION));
        ;
    }

    @Test
    public void should_not_error_if_no_handlers_are_provided() {
        PreSubmitCallbackDispatcher<CaseData> preSubmitCallbackDispatcher =
                new PreSubmitCallbackDispatcher<>(Collections.emptyList());

        assertDoesNotThrow(() -> {
            PreSubmitCallbackResponse<CaseData> callbackResponse =
                    preSubmitCallbackDispatcher
                            .handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
            assertNotNull(callbackResponse);
            assertEquals(caseData, callbackResponse.getData());
            assertTrue(callbackResponse.getErrors().isEmpty());
        });
    }

    @Test
    public void should_not_allow_null_arguments() {
        assertThatThrownBy(() ->
                preSubmitCallbackDispatcher.handle(ABOUT_TO_SUBMIT, null, USER_AUTHORISATION))
                .hasMessage("callback must not be null")
                .isExactlyInstanceOf(NullPointerException.class);
    }
}
