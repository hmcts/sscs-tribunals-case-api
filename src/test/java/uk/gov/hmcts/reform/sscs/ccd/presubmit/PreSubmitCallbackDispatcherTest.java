package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class PreSubmitCallbackDispatcherTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    @Mock private PreSubmitCallbackHandler<CaseData> handler1;
    @Mock private PreSubmitCallbackHandler<CaseData> handler2;
    @Mock private Callback<CaseData> callback;
    @Mock private CaseDetails<CaseData> caseDetails;
    @Mock private CaseData caseData;
    @Mock private CaseData caseDataMutation1;
    @Mock private CaseData caseDataMutation2;
    @Mock private PreSubmitCallbackResponse<CaseData> response1;
    @Mock private PreSubmitCallbackResponse<CaseData> response2;

    private PreSubmitCallbackDispatcher<CaseData> preSubmitCallbackDispatcher;

    @Before
    public void setUp() {
        preSubmitCallbackDispatcher = new PreSubmitCallbackDispatcher<>(
            Arrays.asList(
                handler1,
                handler2
            )
        );
    }

    @Test
    public void should_dispatch_callback_to_handlers_collecting_any_error_messages() {

        when(caseDetails.getCaseData()).thenReturn(caseData);

        when(callback.getCaseDetails()).thenReturn(caseDetails);

        when(response1.getData()).thenReturn(caseDataMutation1);
        when(response1.getErrors()).thenReturn(ImmutableSet.of("error1"));

        when(response2.getData()).thenReturn(caseDataMutation2);
        when(response2.getErrors()).thenReturn(ImmutableSet.of("error2", "error3"));

        when(handler1.canHandle(any(CallbackType.class), any(Callback.class))).thenReturn(true);
        when(handler1.handle(any(CallbackType.class), any(Callback.class), anyString())).thenReturn(response1);

        when(handler2.canHandle(any(CallbackType.class), any(Callback.class))).thenReturn(true);
        when(handler2.handle(any(CallbackType.class), any(Callback.class), anyString())).thenReturn(response2);

        PreSubmitCallbackResponse<CaseData> callbackResponse =
            preSubmitCallbackDispatcher.handle(CallbackType.ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNotNull(callbackResponse);
        assertEquals(caseDataMutation2, callbackResponse.getData());
        Set<String> expectedErrors = ImmutableSet.of("error1", "error2", "error3");

        assertThat(callbackResponse.getErrors(), is(expectedErrors));

        verify(handler1).canHandle(any(CallbackType.class), any(Callback.class));
        verify(handler1).handle(any(CallbackType.class), any(Callback.class), anyString());

        verify(handler2).canHandle(any(CallbackType.class), any(Callback.class));
        verify(handler2).handle(any(CallbackType.class), any(Callback.class), anyString());
    }

    @Test
    public void should_only_dispatch_callback_to_handlers_that_can_handle_it() {

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(caseData);

        when(handler1.canHandle(any(CallbackType.class), any(Callback.class))).thenReturn(false);

        when(handler2.canHandle(any(CallbackType.class), any(Callback.class))).thenReturn(false);

        PreSubmitCallbackResponse<CaseData> callbackResponse =
            preSubmitCallbackDispatcher.handle(CallbackType.ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertNotNull(callbackResponse);
        assertEquals(caseData, callbackResponse.getData());
        assertTrue(callbackResponse.getErrors().isEmpty());

        verify(handler1).canHandle(any(CallbackType.class), any(Callback.class));
        verify(handler1, times(0)).handle(any(CallbackType.class), any(Callback.class), anyString());

        verify(handler2).canHandle(any(CallbackType.class), any(Callback.class));
        verify(handler2, times(0)).handle(any(CallbackType.class), any(Callback.class), anyString());
    }

    @Test
    public void should_not_error_if_no_handlers_are_provided() {

        PreSubmitCallbackDispatcher<CaseData> preSubmitCallbackDispatcher =
            new PreSubmitCallbackDispatcher<>(Collections.emptyList());

        try {

            when(callback.getCaseDetails()).thenReturn(caseDetails);
            when(caseDetails.getCaseData()).thenReturn(caseData);

            PreSubmitCallbackResponse<CaseData> callbackResponse =
                preSubmitCallbackDispatcher
                    .handle(CallbackType.ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

            assertNotNull(callbackResponse);
            assertEquals(caseData, callbackResponse.getData());
            assertTrue(callbackResponse.getErrors().isEmpty());
        } catch (Exception e) {
            fail("Should not have thrown any exception");
        }
    }

    @Test
    public void should_not_allow_null_arguments() {

        assertThatThrownBy(() -> preSubmitCallbackDispatcher.handle(CallbackType.ABOUT_TO_SUBMIT, null, USER_AUTHORISATION))
            .hasMessage("callback must not be null")
            .isExactlyInstanceOf(NullPointerException.class);
    }
}
