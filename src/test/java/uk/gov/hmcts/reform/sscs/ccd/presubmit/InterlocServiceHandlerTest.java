package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState.WELSH_TRANSLATION;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
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
        "TCW_DIRECTION_ISSUED, AWAITING_INFORMATION",
        "JUDGE_DIRECTION_ISSUED, AWAITING_INFORMATION",
        "TCW_DECISION_APPEAL_TO_PROCEED, NONE",
        "JUDGE_DECISION_APPEAL_TO_PROCEED, NONE",
        "SEND_TO_ADMIN, AWAITING_ADMIN_ACTION"
    })
    public void givenEvent_thenSetInterlocReviewStateToExpected(EventType eventType,
                                                                InterlocReviewState expectedInterlocReviewState) {
        when(callback.getEvent()).thenReturn(eventType);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(expectedInterlocReviewState));
    }

    @Test
    @Parameters({
        "TCW_REFER_TO_JUDGE, REVIEW_BY_JUDGE",
        "DWP_CHALLENGE_VALIDITY, REVIEW_BY_TCW",
        "DWP_REQUEST_TIME_EXTENSION, REVIEW_BY_TCW"
    })
    public void givenEvent_thenSetInterlocReviewStateAndSetInterlocReferralDateToExpectedAndDoNotClearDirectionDueDate(
        EventType eventType,
        InterlocReviewState expectedInterlocReviewState) {

        when(callback.getEvent()).thenReturn(eventType);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(expectedInterlocReviewState));
        assertThat(response.getData().getInterlocReferralDate(), is(LocalDate.now()));
        assertThat(response.getData().getDirectionDueDate(), is("01/02/2020"));
    }

    @Test
    @Parameters({
        "NON_COMPLIANT, REVIEW_BY_TCW",
        "NON_COMPLIANT_SEND_TO_INTERLOC, REVIEW_BY_TCW",
        "REINSTATE_APPEAL, AWAITING_ADMIN_ACTION"
    })
    public void givenEvent_thenSetInterlocReviewStateAndSetInterlocReferralDateToExpectedAndClearDirectionDueDate(
            EventType eventType,
            InterlocReviewState expectedInterlocReviewState) {

        when(callback.getEvent()).thenReturn(eventType);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(expectedInterlocReviewState));
        assertThat(response.getData().getInterlocReferralDate(), is(LocalDate.now()));
        assertNull(response.getData().getDirectionDueDate());
    }

    @Test
    @Parameters({
        "NON_COMPLIANT, REVIEW_BY_TCW",
        "NON_COMPLIANT_SEND_TO_INTERLOC, REVIEW_BY_TCW",
        "REINSTATE_APPEAL, AWAITING_ADMIN_ACTION"
    })
    public void givenWelshCaseEvent_thenSetInterlocReviewStateAndSetInterlocReferralDateToExpectedAndClearDirectionDueDate(
            EventType eventType,
            InterlocReviewState expectedInterlocReviewState) {

        sscsCaseData.setLanguagePreferenceWelsh("Yes");
        when(callback.getEvent()).thenReturn(eventType);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getInterlocReviewState(), is(WELSH_TRANSLATION));
        assertThat(response.getData().getWelshInterlocNextReviewState(), is(expectedInterlocReviewState.getCcdDefinition()));
        assertThat(response.getData().getInterlocReferralDate(), is(LocalDate.now()));
        assertNull(response.getData().getDirectionDueDate());
    }

    @Test(expected = IllegalStateException.class)
    public void throwExceptionIfCannotHandleEventType() {
        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);

        sscsCaseData = SscsCaseData.builder().interlocReviewState(InterlocReviewState.REVIEW_BY_JUDGE).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }

}
