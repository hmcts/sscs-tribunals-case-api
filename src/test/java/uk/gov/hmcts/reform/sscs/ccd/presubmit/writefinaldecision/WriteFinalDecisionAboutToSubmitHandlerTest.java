package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.MID_EVENT;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;

@RunWith(JUnitParamsRunner.class)
public class WriteFinalDecisionAboutToSubmitHandlerTest {
    private static final String USER_AUTHORISATION = "Bearer token";
    private WriteFinalDecisionAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private DecisionNoticeQuestionService decisionNoticeQuestionService;
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        decisionNoticeQuestionService = new DecisionNoticeQuestionService();
        handler = new WriteFinalDecisionAboutToSubmitHandler(decisionNoticeQuestionService);

        when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
                .appeal(Appeal.builder().build())
                .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonWriteFinalDecisionEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    public void getStandardRateSelectedAndDailyLivingPointsAreTooLow_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        sscsCaseData.setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have previously selected a standard rate award for Daily Living. The points awarded don't match. Please review your previous selection.", error);
    }

    @Test
    public void getStandardRateSelectedAndDailyLivingPointsAreTooHigh_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood", "takingNutrition"));

        sscsCaseData.setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f");
        sscsCaseData.setPipWriteFinalDecisionTakingNutritionQuestion("takingNutrition2f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have previously selected a standard rate award for Daily Living. The points awarded don't match. Please review your previous selection.", error);
    }

    @Test
    public void getStandardRateSelectedAndDailyLivingPointsAreJustRight_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        sscsCaseData.setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

    }

    @Test
    @Parameters({"ABOUT_TO_START", "MID_EVENT", "SUBMITTED"})
    public void givenANonCallbackType_thenReturnFalse(CallbackType callbackType) {
        assertFalse(handler.canHandle(callbackType, callback));
    }

    @Test(expected = IllegalStateException.class)
    public void throwsExceptionIfItCannotHandleTheAppeal() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);
    }
}
