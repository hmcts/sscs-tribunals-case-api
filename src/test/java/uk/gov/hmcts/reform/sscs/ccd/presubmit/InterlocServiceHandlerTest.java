package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;

import java.time.LocalDate;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;


@RunWith(JUnitParamsRunner.class)
public class InterlocServiceHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private InterlocServiceHandler handler;

    private SscsCaseData sscsCaseData;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private Callback<SscsCaseData> callback;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new InterlocServiceHandler();

        sscsCaseData = SscsCaseData.builder().directionDueDate("01/02/2020").build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    @Parameters({
        "TCW_DIRECTION_ISSUED, ABOUT_TO_SUBMIT, true",
        "JUDGE_DIRECTION_ISSUED, ABOUT_TO_SUBMIT, true",
        "TCW_REFER_TO_JUDGE, ABOUT_TO_SUBMIT, true",
        "NON_COMPLIANT, ABOUT_TO_SUBMIT, true",
        "NON_COMPLIANT_SEND_TO_INTERLOC, ABOUT_TO_SUBMIT, true",
        "REINSTATE_APPEAL, ABOUT_TO_SUBMIT, true",
        "TCW_DECISION_APPEAL_TO_PROCEED, ABOUT_TO_SUBMIT, true",
        "JUDGE_DECISION_APPEAL_TO_PROCEED, ABOUT_TO_SUBMIT, true",
        "SEND_TO_ADMIN, ABOUT_TO_SUBMIT, true",
        "DWP_CHALLENGE_VALIDITY, ABOUT_TO_SUBMIT, true",
        "DWP_CHALLENGE_VALIDITY, ABOUT_TO_START, false",
        "DWP_CHALLENGE_VALIDITY, SUBMITTED, false",
        "DWP_CHALLENGE_VALIDITY, MID_EVENT, false",
    })
    public void givenEvent_thenCanHandle(EventType eventType, CallbackType callbackType, boolean expected) {
        when(callback.getEvent()).thenReturn(eventType);
        assertEquals(expected, handler.canHandle(callbackType, callback));
    }

    @Test(expected = NullPointerException.class)
    public void givenNullCallback_shouldThrowException() {
        handler.canHandle(ABOUT_TO_SUBMIT, null);
    }

    @Test
    @Parameters({
        "TCW_DIRECTION_ISSUED, awaitingInformation",
        "JUDGE_DIRECTION_ISSUED, awaitingInformation",
        "TCW_DECISION_APPEAL_TO_PROCEED, none",
        "JUDGE_DECISION_APPEAL_TO_PROCEED, none",
        "SEND_TO_ADMIN, awaitingAdminAction"
    })
    public void givenEvent_thenSetInterlocReviewStateToExpected(EventType eventType,
                                                                String expectedInterlocReviewState) {
        when(callback.getEvent()).thenReturn(eventType);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(expectedInterlocReviewState));
    }

    @Test
    @Parameters({
        "TCW_REFER_TO_JUDGE, reviewByJudge",
        "DWP_CHALLENGE_VALIDITY, reviewByTcw",
        "DWP_REQUEST_TIME_EXTENSION, reviewByTcw"
    })
    public void givenEvent_thenSetInterlocReviewStateAndSetInterlocReferralDateToExpectedAndDoNotClearDirectionDueDate(
        EventType eventType,
        String expectedInterlocReviewState) {

        when(callback.getEvent()).thenReturn(eventType);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(expectedInterlocReviewState));
        assertThat(response.getData().getInterlocReferralDate(), is(LocalDate.now().toString()));
        assertThat(response.getData().getDirectionDueDate(), is("01/02/2020"));
    }

    @Test
    @Parameters({
            "NON_COMPLIANT, reviewByTcw",
            "NON_COMPLIANT_SEND_TO_INTERLOC, reviewByTcw",
            "REINSTATE_APPEAL, awaitingAdminAction"
    })
    public void givenEvent_thenSetInterlocReviewStateAndSetInterlocReferralDateToExpectedAndClearDirectionDueDate(
            EventType eventType,
            String expectedInterlocReviewState) {

        when(callback.getEvent()).thenReturn(eventType);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(expectedInterlocReviewState));
        assertThat(response.getData().getInterlocReferralDate(), is(LocalDate.now().toString()));
        assertNull(response.getData().getDirectionDueDate());
    }

    @Test
    @Parameters({
            "NON_COMPLIANT, reviewByTcw",
            "NON_COMPLIANT_SEND_TO_INTERLOC, reviewByTcw",
            "REINSTATE_APPEAL, awaitingAdminAction"
    })
    public void givenWelshCaseEvent_thenSetInterlocReviewStateAndSetInterlocReferralDateToExpectedAndClearDirectionDueDate(
            EventType eventType,
            String expectedInterlocReviewState) {

        sscsCaseData.setLanguagePreferenceWelsh("Yes");
        when(callback.getEvent()).thenReturn(eventType);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(InterlocReviewState.WELSH_TRANSLATION.getId()));
        assertThat(response.getData().getWelshInterlocNextReviewState(), is(expectedInterlocReviewState));
        assertThat(response.getData().getInterlocReferralDate(), is(LocalDate.now().toString()));
        assertNull(response.getData().getDirectionDueDate());
    }

    @Test(expected = IllegalStateException.class)
    public void throwExceptionIfCannotHandleEventType() {
        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);

        sscsCaseData = SscsCaseData.builder().interlocReviewState("someValue").build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

}