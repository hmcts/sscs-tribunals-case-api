package uk.gov.hmcts.reform.sscs.ccd.presubmit.issuefinaldecision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.SUBMITTED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.Optional;
import javax.validation.Validation;
import javax.validation.Validator;
import junitparams.JUnitParamsRunner;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdCallbackMapService;
import uk.gov.hmcts.reform.sscs.service.*;

@RunWith(JUnitParamsRunner.class)
public class IssueFinalDecisionSubmittedHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private IssueFinalDecisionSubmittedHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;
    @Mock
    private CaseDetails<SscsCaseData> caseDetails;
    @Mock
    private CcdCallbackMapService ccdCallbackMapService;
    @Mock
    private CallbackOrchestratorService callbackOrchestratorService;

    private SscsCaseData sscsCaseData;

    protected static Validator validator = Validation.byDefaultProvider()
            .configure()
            .messageInterpolator(new ParameterMessageInterpolator())
            .buildValidatorFactory()
            .getValidator();

    @Before
    public void setUp() {
        openMocks(this);

        handler = new IssueFinalDecisionSubmittedHandler(ccdCallbackMapService, callbackOrchestratorService, true);

        when(callback.getEvent()).thenReturn(EventType.ISSUE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetails));

        sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("ccdId")
                .build();

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
        when(caseDetails.getState()).thenReturn(State.HEARING);
    }

    @Test
    public void givenANonIssueFinalDecisionEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(SUBMITTED, callback));
    }

    @Test
    public void givenANonSubmittedEvent_thenReturnFalse() {
        assertThrows(IllegalStateException.class, () -> handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION));
    }

    @Test
    public void givenPostHearingsFlagIsFalse_thenOnlyCallCallback() {
        handler = new IssueFinalDecisionSubmittedHandler(ccdCallbackMapService, callbackOrchestratorService, false);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(callbackOrchestratorService, times(1)).sendMessageToCallbackOrchestrator(callback);
        assertThat(response.getErrors()).isEmpty();
    }

    @Test
    public void givenPostHearingsFlagIsTrueAndCorrectionInProgress_thenHandleCallbackMap() {
        sscsCaseData.getPostHearing().getCorrection().setCorrectionFinalDecisionInProgress(YES);

        SscsCaseData newCaseData = SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .postHearing(PostHearing.builder()
                .correction(Correction.builder()
                    .correctionFinalDecisionInProgress(NO)
                    .build())
                .build())
            .build();

        when(ccdCallbackMapService.handleCcdCallbackMap(CorrectionActions.GRANT, sscsCaseData)).thenReturn(newCaseData);

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(callbackOrchestratorService, times(1)).sendMessageToCallbackOrchestrator(callback);
        verify(ccdCallbackMapService, times(1)).handleCcdCallbackMap(CorrectionActions.GRANT, sscsCaseData);
        assertThat(response.getErrors()).isEmpty();
        assertThat(response.getData().getPostHearing().getCorrection().getCorrectionFinalDecisionInProgress()).isEqualTo(NO);
    }

    @Test
    public void givenPostHearingsFlagIsTrueAndCorrectionNotInProgress_thenOnlyCallCallback() {
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(SUBMITTED, callback, USER_AUTHORISATION);

        verify(callbackOrchestratorService, times(1)).sendMessageToCallbackOrchestrator(callback);
        verify(ccdCallbackMapService, times(0)).handleCcdCallbackMap(CorrectionActions.GRANT, sscsCaseData);
        assertThat(response.getErrors()).isEmpty();
    }
}