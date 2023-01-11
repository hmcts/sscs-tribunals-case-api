package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class OutcomeServiceHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";

    private OutcomeServiceHandler handler;

    private SscsCaseData sscsCaseData;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    @Mock
    private Callback<SscsCaseData> callback;

    @Before
    public void setUp() {
        openMocks(this);
        handler = new OutcomeServiceHandler();

        sscsCaseData = SscsCaseData.builder().build();

        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenTcwDecisionStrikeOut_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.TCW_DECISION_STRIKE_OUT);

        assertTrue(handler.canHandle(CallbackType.ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenJudgeDecisionStrikeoutEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.JUDGE_DECISION_STRIKEOUT);

        assertTrue(handler.canHandle(CallbackType.ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenReinstateAppealEvent_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.REINSTATE_APPEAL);

        assertTrue(handler.canHandle(CallbackType.ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenCohDecisionIssued_thenReturnTrue() {
        when(callback.getEvent()).thenReturn(EventType.COH_DECISION_ISSUED);

        assertTrue(handler.canHandle(CallbackType.ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void setsOutcomeForTcwDecisionStrikeOut() {
        when(callback.getEvent()).thenReturn(EventType.TCW_DECISION_STRIKE_OUT);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getOutcome(), is("nonCompliantAppealStruckout"));
    }

    @Test
    public void setsOutcomeForJudgeDecisionStrikeout() {
        when(callback.getEvent()).thenReturn(EventType.JUDGE_DECISION_STRIKEOUT);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getOutcome(), is("nonCompliantAppealStruckout"));
    }

    @Test
    public void setsOutcomeForReinstateAppeal() {
        when(callback.getEvent()).thenReturn(EventType.REINSTATE_APPEAL);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getOutcome(), is("reinstated"));
    }

    @Test
    public void setsOutcomeForCohDecisionIssued() {
        when(callback.getEvent()).thenReturn(EventType.COH_DECISION_ISSUED);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertThat(response.getData().getOutcome(), is("decisionUpheld"));
    }

    @Test(expected = IllegalStateException.class)
    public void throwExceptionIfCannotHandleEventType() {
        when(callback.getEvent()).thenReturn(EventType.CASE_UPDATED);

        sscsCaseData = SscsCaseData.builder().interlocReviewState(InterlocReviewState.REVIEW_BY_TCW).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
