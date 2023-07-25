package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_START;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.idam.UserRole;
import uk.gov.hmcts.reform.sscs.service.UserDetailsService;

@RunWith(JUnitParamsRunner.class)
public class WriteFinalDecisionAboutToStartHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private WriteFinalDecisionAboutToStartHandler handler;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Before
    public void setUp() {
        openMocks(this);

        when(caseDetails.getCaseData()).thenReturn(new SscsCaseData());
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);

        handler = new WriteFinalDecisionAboutToStartHandler(userDetailsService, true);
    }

    @Test
    @Parameters({"MID_EVENT", "ABOUT_TO_SUBMIT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

    @Test
    public void givenNullCaseDetails_thenReturnFalse() {
        when(callback.getCaseDetails()).thenReturn(null);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenNullCaseData_thenReturnFalse() {
        when(caseDetails.getCaseData()).thenReturn(null);
        assertFalse(handler.canHandle(ABOUT_TO_START, callback));
    }

    @Test
    public void givenPostHearingsFalse_thenErrorsShouldBeEmpty() {
        handler = new WriteFinalDecisionAboutToStartHandler(userDetailsService, false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(response.getErrors().size(), 0);
    }

    @Test
    @Parameters({"POST_HEARING", "DORMANT_APPEAL_STATE"})
    public void givenStateIsDormantOrPostHearingsAndIsSalariedJudge_thenErrorsShouldBeEmpty(State state) {
        when(caseDetails.getState()).thenReturn(state);
        when(userDetailsService.getUserRoles(USER_AUTHORISATION)).thenReturn(List.of(UserRole.SALARIED_JUDGE.getValue()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(response.getErrors().size(), 0);
    }

    @Test
    public void givenStateIsNorDormantOrPostHearings_thenErrorsShouldBeEmpty() {
        when(caseDetails.getState()).thenReturn(State.VALID_APPEAL);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        assertEquals(response.getErrors().size(), 0);
    }

    @Test
    public void givenStateIsDormantAndIsntSalariedJudge_thenThrowError() {
        when(caseDetails.getState()).thenReturn(State.DORMANT_APPEAL_STATE);
        when(userDetailsService.getUserRoles(USER_AUTHORISATION)).thenReturn(List.of(UserRole.JUDGE.getValue()));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_START, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You do not have access to proceed", error);
    }
}