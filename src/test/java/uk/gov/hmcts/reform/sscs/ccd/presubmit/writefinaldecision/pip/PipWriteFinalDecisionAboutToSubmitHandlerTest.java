package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import junitparams.JUnitParamsRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.PreSubmitCallbackResponse;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocument;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsDocumentDetails;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionAboutToSubmitHandlerTestBase;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.PipDecisionNoticeOutcomeService;
import uk.gov.hmcts.reform.sscs.service.PipDecisionNoticeQuestionService;

@RunWith(JUnitParamsRunner.class)
public class PipWriteFinalDecisionAboutToSubmitHandlerTest extends WriteFinalDecisionAboutToSubmitHandlerTestBase<PipDecisionNoticeQuestionService> {

    public PipWriteFinalDecisionAboutToSubmitHandlerTest() throws IOException {
        super(new PipDecisionNoticeQuestionService());
    }

    @Override
    protected DecisionNoticeOutcomeService createOutcomeService(PipDecisionNoticeQuestionService decisionNoticeQuestionService) {
        return new PipDecisionNoticeOutcomeService(decisionNoticeQuestionService);
    }

    @Override
    protected void setValidPointsAndActivitiesScenario(SscsCaseData caseData, String descriptorFlowValue) {

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow(descriptorFlowValue);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("noAward");

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));


        // 8 points - correct for daily living standard award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points

        // 0 points - correct for mobility no award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMovingAroundQuestion("movingAround12a");

    }

    @Override
    public void givenDraftFinalDecisionAlreadyExistsOnCase_thenOverwriteExistingDraft() {
        SscsDocument doc = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("oldDraft.doc").documentType(DRAFT_DECISION_NOTICE.getValue()).build()).build();
        List<SscsDocument> docs = new ArrayList<>();
        docs.add(doc);
        callback.getCaseDetails().getCaseData().setSscsDocument(docs);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);

        // Why do we not need to set valid scenario ?

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getData().getSscsDocument().size());
        assertEquals((String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))), response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
    }

    @Test
    public void givenMobilityStandardRateCorrectAndDailyLivingStandardRateSelectedAndDailyLivingPointsAreTooLow_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("standardRate");

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        // 8 points - correct for mobility standard award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");

        // 0 points - too low for daily living standard award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have previously selected a standard rate award for Daily Living. The points awarded don't match. Please review your previous selection.", error);
    }

    @Test
    public void givenMobilityStandardRateCorrectAndDailyLivingStandardRateSelectedAndDailyLivingPointsAreTooHigh_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("standardRate");

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood", "takingNutrition"));

        // 8 points - correct for mobility standard award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");

        // 18 points total - too high for daily living standard award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionTakingNutritionQuestion("takingNutrition2f"); // 10 points


        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have previously selected a standard rate award for Daily Living. The points awarded don't match. Please review your previous selection.", error);
    }

    @Test
    public void givenMobilityStandardRateCorrectAndDailyLivingStandardRateSelectedAndDailyLivingPointsAreJustRight_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("standardRate");

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood", "takingNutrition"));

        // 8 points - correct for mobility standard award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");

        // 8 points total - correct for daily living standard award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

    }

    @Test
    public void givenMobilityStandardRateCorrectAndDailyLivingEnhancedRateSelectedAndDailyLivingPointsAreTooLow_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("enhancedRate");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("standardRate");

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        // 8 points - correct for mobility standard award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");

        // 8 points - too low for daily living enhanced award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have previously selected an enhanced rate award for Daily Living. The points awarded don't match. Please review your previous selection.", error);
    }


    @Test
    public void givenMobilityStandardRateCorrectAndDailyLivingEnhancedRateSelectedAndDailyLivingPointsAreJustRight_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("enhancedRate");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("standardRate");

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood", "takingNutrition"));

        // 8 points - correct for mobility standard award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");

        // 18 points total - correct for daily living enhanced award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionTakingNutritionQuestion("takingNutrition2f"); // 10 points


        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

    }

    @Test
    public void givenMobilityStandardRateCorrectAndDailyLivingNoAwardSelectedAndDailyLivingPointsAreTooHigh_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("noAward");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("standardRate");

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood", "takingNutrition"));

        // 8 points - correct for mobility standard award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");

        // 8 points total - too high for no  award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points


        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have previously selected No Award for Daily Living. The points awarded don't match. Please review your previous selection.", error);
    }

    @Test
    public void givenMobilityStandardRateCorrectAndDailyLivingNoAwardSelectedAndDailyLivingPointsAreJustRight_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("noAward");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("standardRate");

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood", "takingNutrition"));

        // 8 points - correct for mobility standard award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");

        // 0 points total - correct for daily living no award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        Iterator<String> iter = response.getErrors().iterator();
        while (iter.hasNext()) {
            System.out.println(iter.next());
        }

        assertEquals(0, response.getErrors().size());

    }


    @Test
    public void givenDailyLivingStandardRateCorrectAndMobilityStandardRateSelectedAndMobilityPointsAreTooLow_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("standardRate");

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        // 8 points - correct for daily living standard award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points

        // 0 points - too low for mobility standard award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMovingAroundQuestion("movingAround12a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have previously selected a standard rate award for Mobility. The points awarded don't match. Please review your previous selection.", error);
    }

    @Test
    public void givenDailyLivingStandardRateCorrectAndMobilityStandardRateSelectedAndMobilityPointsAreTooHigh_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("standardRate");

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        // 8 points - correct for daily living standard award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points

        // 12 points - too high for mobility standard award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMovingAroundQuestion("movingAround12f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have previously selected a standard rate award for Mobility. The points awarded don't match. Please review your previous selection.", error);
    }

    @Test
    public void givenDailyLivingStandardRateCorrectAndDailyMobilityStandardRateSelectedAndDailyMobilityPointsAreJustRight_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("standardRate");

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        // 8 points - correct for daily living standard award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points

        // 8 points - correct for mobility standard award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMovingAroundQuestion("movingAround12d");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

    }

    @Test
    public void givenDailyLivingStandardRateCorrectAndMobilityEnhancedRateSelectedAndMobilityPointsAreTooLow_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("enhancedRate");

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        // 8 points - correct for daily living standard award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points

        // 8 points - too low for mobility enhanced award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMovingAroundQuestion("movingAround12c");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have previously selected an enhanced rate award for Mobility. The points awarded don't match. Please review your previous selection.", error);
    }


    @Test
    public void givenDailyLivingStandardRateCorrectAndMobilityEnhancedRateSelectedAndMobilityPointsAreJustRight_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("enhancedRate");

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        // 8 points - correct for daily living standard award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points

        // 12 points - correct for mobility enhanced award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMovingAroundQuestion("movingAround12f");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

    }

    @Test
    public void givenDailyLivingStandardRateCorrectAndMobilityNoAwardSelectedAndMobilityPointsAreTooHigh_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("noAward");

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        // 8 points - correct for daily living standard award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points

        // 8 points - too high for mobility no award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMovingAroundQuestion("movingAround12c");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        String error = response.getErrors().stream().findFirst().orElse("");
        assertEquals("You have previously selected No Award for Mobility. The points awarded don't match. Please review your previous selection.", error);
    }

    @Test
    public void givenDailyLivingStandardRateCorrectAndMobilityNoAwardSelectedAndMobilityPointsAreJustRight_thenDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("noAward");

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));


        // 8 points - correct for daily living standard award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f"); // 8 points

        // 0 points - correct for mobility no award
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMovingAroundQuestion("movingAround12a");

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(0, response.getErrors().size());

    }

    @Test
    public void givenBothDailyLivingAndMobilityPointsAreIncorrect_thenDisplayTwoErrors() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.getSscsFinalDecisionCaseData().setWriteFinalDecisionGenerateNotice(YES);
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityQuestion("standardRate");

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionDailyLivingActivitiesQuestion(
            Arrays.asList("preparingFood"));

        sscsCaseData.getSscsPipCaseData().setPipWriteFinalDecisionMobilityActivitiesQuestion(
            Arrays.asList("movingAround"));

        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(2, response.getErrors().size());

        Iterator<String> iterator = response.getErrors().iterator();
        String error1 = iterator.next();
        String error2 = iterator.next();
        assertEquals("You have previously selected a standard rate award for Daily Living. The points awarded don't match. Please review your previous selection.", error1);
        assertEquals("You have previously selected a standard rate award for Mobility. The points awarded don't match. Please review your previous selection.", error2);

    }
}
