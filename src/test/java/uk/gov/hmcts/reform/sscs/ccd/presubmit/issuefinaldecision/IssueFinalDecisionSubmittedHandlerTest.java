package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CorrectionActions;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdCallbackMapService;

public class IssueFinalDecisionSubmittedHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private SscsCaseData sscsCaseData;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CcdCallbackMapService ccdCallbackMapService;
    private IssueFinalDecisionSubmittedHandler handler;

    @BeforeEach
    public void setUp() {
        openMocks(this);

        handler = new IssueFinalDecisionSubmittedHandler(ccdCallbackMapService, true);
        sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .state(State.POST_HEARING).build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetails.getState()).thenReturn(State.POST_HEARING);
        when(callback.getEvent()).thenReturn(EventType.ISSUE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
    }

    @Test
    void throwsExceptionIfItCannotHandleTheEvent() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);

        assertThatThrownBy(() -> handler.handle(MID_EVENT, callback, USER_AUTHORISATION))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void givenANonIssueFinalDecisionCaseEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.ADJOURN_CASE);
        assertThat(handler.canHandle(MID_EVENT, callback)).isFalse();
    }

    @Test
    void givenCaseReadyForPostHearings_thenGrantCorrection() {
        when(ccdCallbackMapService.handleCcdCallbackMap(CorrectionActions.GRANT, sscsCaseData)).thenReturn(sscsCaseData);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(ccdCallbackMapService, times(1)).handleCcdCallbackMap(CorrectionActions.GRANT, sscsCaseData);
        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    void givenCaseReadyForPostHearingsAndFlagIsFalse_thenDontGrantCorrection() {
        handler = new IssueFinalDecisionSubmittedHandler(ccdCallbackMapService, false);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(ccdCallbackMapService, times(0)).handleCcdCallbackMap(CorrectionActions.GRANT, sscsCaseData);
        assertThat(response.getErrors()).isEmpty();
    }
}
