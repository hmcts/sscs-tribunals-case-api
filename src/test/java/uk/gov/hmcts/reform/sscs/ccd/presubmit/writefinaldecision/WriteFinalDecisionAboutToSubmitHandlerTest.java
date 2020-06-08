package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.DwpState.FINAL_DECISION_ISSUED;
import static uk.gov.hmcts.reform.sscs.domain.wrapper.ComparedRate.Higher;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
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
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
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
    private DecisionNoticeOutcomeService decisionNoticeOutcomeService;
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() throws IOException {
        initMocks(this);
        decisionNoticeQuestionService = new DecisionNoticeQuestionService();
        decisionNoticeOutcomeService = new DecisionNoticeOutcomeService();
        handler = new WriteFinalDecisionAboutToSubmitHandler(decisionNoticeQuestionService,
            decisionNoticeOutcomeService);

        when(callback.getEvent()).thenReturn(EventType.WRITE_FINAL_DECISION);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        sscsCaseData = SscsCaseData.builder().ccdCaseId("ccdId")
            .appeal(Appeal.builder().build())
            .pipWriteFinalDecisionComparedToDwpDailyLivingQuestion(Higher.getKey())
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion(Higher.getKey())
            .build();
        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);
    }

    @Test
    public void givenANonWriteFinalDecisionEvent_thenReturnFalse() {
        when(callback.getEvent()).thenReturn(EventType.APPEAL_RECEIVED);
        assertFalse(handler.canHandle(ABOUT_TO_SUBMIT, callback));
    }

    @Test
    @Parameters({
            "higher, higher, decisionInFavourOfAppellant",
            "higher, same, decisionInFavourOfAppellant",
            "higher, lower, decisionUpheld",
            "same, higher, decisionInFavourOfAppellant",
            "same, same, decisionUpheld",
            "same, lower, decisionUpheld",
            "lower, higher, decisionUpheld",
            "lower, same, decisionUpheld",
            "lower, lower, decisionUpheld"})
    public void givenFinalDecisionComparedToDwpQuestionAndAtLeastOneDecisionIsHigherAndNeitherIsLower_thenSetDecisionInFavourOfAppellant(String comparedRateDailyLiving, String comparedRateMobility, String expectedOutcome) {
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving);
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(expectedOutcome, response.getData().getOutcome());
        assertEquals(FINAL_DECISION_ISSUED.getId(), response.getData().getDwpState());
    }

    @Test
    public void givenFinalDecisionComparedToDwpQuestionComparedRatesAreNull_thenDisplayAnError() {
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(null);
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("Outcome cannot be empty. Please check case data. If problem continues please contact support", error);
    }

    @Test
    public void givenMobilityStandardRateCorrectAndDailyLivingStandardRateSelectedAndDailyLivingPointsAreTooLow_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("standardRate");

        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        // 8 points - correct for mobility standard award
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");

        // 0 points - too low for daily living standard award
        sscsCaseData.setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have previously selected a standard rate award for Daily Living. The points awarded don't match. Please review your previous selection.", error);
    }

    @Test
    public void givenMobilityStandardRateCorrectAndDailyLivingStandardRateSelectedAndDailyLivingPointsAreTooHigh_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("standardRate");

        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood", "takingNutrition"));

        // 8 points - correct for mobility standard award
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");

        // 18 points total - too high for daily living standard award
        sscsCaseData.setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points
        sscsCaseData.setPipWriteFinalDecisionTakingNutritionQuestion("takingNutrition2f"); // 10 points


        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have previously selected a standard rate award for Daily Living. The points awarded don't match. Please review your previous selection.", error);
    }

    @Test
    public void givenMobilityStandardRateCorrectAndDailyLivingStandardRateSelectedAndDailyLivingPointsAreJustRight_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("standardRate");

        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood", "takingNutrition"));

        // 8 points - correct for mobility standard award
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");

        // 8 points total - correct for daily living standard award
        sscsCaseData.setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

    }

    @Test
    public void givenMobilityStandardRateCorrectAndDailyLivingEnhancedRateSelectedAndDailyLivingPointsAreTooLow_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("enhancedRate");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("standardRate");

        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        // 8 points - correct for mobility standard award
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");

        // 8 points - too low for daily living enhanced award
        sscsCaseData.setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have previously selected an enhanced rate award for Daily Living. The points awarded don't match. Please review your previous selection.", error);
    }


    @Test
    public void givenMobilityStandardRateCorrectAndDailyLivingEnhancedRateSelectedAndDailyLivingPointsAreJustRight_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("enhancedRate");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("standardRate");

        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood", "takingNutrition"));

        // 8 points - correct for mobility standard award
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");

        // 18 points total - correct for daily living enhanced award
        sscsCaseData.setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points
        sscsCaseData.setPipWriteFinalDecisionTakingNutritionQuestion("takingNutrition2f"); // 10 points


        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

    }

    @Test
    public void givenMobilityStandardRateCorrectAndDailyLivingNoAwardSelectedAndDailyLivingPointsAreTooHigh_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("noAward");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("standardRate");

        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood", "takingNutrition"));

        // 8 points - correct for mobility standard award
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");

        // 8 points total - too high for no  award
        sscsCaseData.setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points


        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have previously selected No Award for Daily Living. The points awarded don't match. Please review your previous selection.", error);
    }

    @Test
    public void givenMobilityStandardRateCorrectAndDailyLivingNoAwardSelectedAndDailyLivingPointsAreJustRight_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("noAward");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("standardRate");

        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood", "takingNutrition"));

        // 8 points - correct for mobility standard award
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");

        // 0 points total - correct for daily living no award
        sscsCaseData.setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

    }


    @Test
    public void givenDailyLivingStandardRateCorrectAndMobilityStandardRateSelectedAndMobilityPointsAreTooLow_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("standardRate");

        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        // 8 points - correct for daily living standard award
        sscsCaseData.setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points

        // 0 points - too low for mobility standard award
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion("movingAround12a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have previously selected a standard rate award for Mobility. The points awarded don't match. Please review your previous selection.", error);
    }

    @Test
    public void givenDailyLivingStandardRateCorrectAndMobilityStandardRateSelectedAndMobilityPointsAreTooHigh_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("standardRate");

        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        // 8 points - correct for daily living standard award
        sscsCaseData.setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points

        // 12 points - too high for mobility standard award
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion("movingAround12f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have previously selected a standard rate award for Mobility. The points awarded don't match. Please review your previous selection.", error);
    }

    @Test
    public void givenDailyLivingStandardRateCorrectAndDailyMobilityStandardRateSelectedAndDailyMobilityPointsAreJustRight_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("standardRate");

        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        // 8 points - correct for daily living standard award
        sscsCaseData.setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points

        // 8 points - correct for mobility standard award
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

    }

    @Test
    public void givenDailyLivingStandardRateCorrectAndMobilityEnhancedRateSelectedAndMobilityPointsAreTooLow_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("enhancedRate");

        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        // 8 points - correct for daily living standard award
        sscsCaseData.setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points

        // 8 points - too low for mobility enhanced award
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion("movingAround12c");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have previously selected an enhanced rate award for Mobility. The points awarded don't match. Please review your previous selection.", error);
    }


    @Test
    public void givenDailyLivingStandardRateCorrectAndMobilityEnhancedRateSelectedAndMobilityPointsAreJustRight_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("enhancedRate");

        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        // 8 points - correct for daily living standard award
        sscsCaseData.setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points

        // 12 points - correct for mobility enhanced award
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion("movingAround12f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

    }

    @Test
    public void givenDailyLivingStandardRateCorrectAndMobilityNoAwardSelectedAndMobilityPointsAreTooHigh_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("noAward");

        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        // 8 points - correct for daily living standard award
        sscsCaseData.setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points

        // 8 points - too high for mobility no award
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion("movingAround12c");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have previously selected No Award for Mobility. The points awarded don't match. Please review your previous selection.", error);
    }

    @Test
    public void givenDailyLivingStandardRateCorrectAndMobilityNoAwardSelectedAndMobilityPointsAreJustRight_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("noAward");

        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));


        // 8 points - correct for daily living standard award
        sscsCaseData.setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points

        // 0 points - correct for mobility no award
        sscsCaseData.setPipWriteFinalDecisionMovingAroundQuestion("movingAround12a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

    }

    @Test
    public void givenBothDailyLivingAndMobilityPointsAreIncorrect_thenDisplayTwoErrors() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("standardRate");

        sscsCaseData.setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        sscsCaseData.setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(2, response.getErrors().size());

        Iterator<String> iterator = response.getErrors().iterator();
        String error1 = iterator.next();
        String error2 = iterator.next();
        assertEquals("You have previously selected a standard rate award for Daily Living. The points awarded don't match. Please review your previous selection.", error1);
        assertEquals("You have previously selected a standard rate award for Mobility. The points awarded don't match. Please review your previous selection.", error2);

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
