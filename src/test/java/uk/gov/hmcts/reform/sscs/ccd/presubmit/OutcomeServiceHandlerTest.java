package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.time.LocalDateTime.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.CASE_UPDATED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.COH_DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.JUDGE_DECISION_STRIKEOUT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.TCW_DECISION_STRIKE_OUT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.WITH_DWP;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.InterlocReviewState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@ExtendWith(MockitoExtension.class)
public class OutcomeServiceHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private SscsCaseData sscsCaseData;
    private CaseDetails<SscsCaseData> caseDetails;
    private Callback<SscsCaseData> callback;

    private OutcomeServiceHandler handler;

    @BeforeEach
    public void setUp() {
        sscsCaseData = SscsCaseData.builder().build();
        caseDetails = new CaseDetails<>(123L, "SSCS", WITH_DWP, sscsCaseData, now(), "Benefit");

        handler = new OutcomeServiceHandler();
    }

    @Test
    public void givenTcwDecisionStrikeOut_thenReturnTrue() {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), TCW_DECISION_STRIKE_OUT, false);

        assertTrue(handler.canHandle(CallbackType.ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenJudgeDecisionStrikeoutEvent_thenReturnTrue() {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), JUDGE_DECISION_STRIKEOUT, false);

        assertTrue(handler.canHandle(CallbackType.ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void givenCohDecisionIssued_thenReturnTrue() {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), COH_DECISION_ISSUED, false);

        assertTrue(handler.canHandle(CallbackType.ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void setsOutcomeForTcwDecisionStrikeOut() {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), TCW_DECISION_STRIKE_OUT, false);

        var response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("nonCompliantAppealStruckout", response.getData().getOutcome());
    }

    @Test
    public void setsOutcomeForJudgeDecisionStrikeout() {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), JUDGE_DECISION_STRIKEOUT, false);

        var response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("nonCompliantAppealStruckout", response.getData().getOutcome());
    }

    @Test
    public void setsOutcomeForCohDecisionIssued() {
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), COH_DECISION_ISSUED, false);

        var response = handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals("decisionUpheld", response.getData().getOutcome());
    }

    @Test
    public void throwExceptionIfCannotHandleEventType() {
        sscsCaseData = SscsCaseData.builder().interlocReviewState(InterlocReviewState.REVIEW_BY_TCW).build();
        callback = new Callback<>(caseDetails, Optional.of(caseDetails), CASE_UPDATED, false);

        assertThrows(IllegalStateException.class,
                () -> handler.handle(CallbackType.ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }
}
