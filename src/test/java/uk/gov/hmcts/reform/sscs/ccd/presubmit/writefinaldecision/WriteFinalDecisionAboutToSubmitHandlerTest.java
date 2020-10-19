package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static uk.gov.hmcts.reform.sscs.ccd.callback.CallbackType.ABOUT_TO_SUBMIT;
import static uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType.DRAFT_DECISION_NOTICE;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
import uk.gov.hmcts.reform.sscs.service.PipDecisionNoticeQuestionService;
import uk.gov.hmcts.reform.sscs.service.PreviewDocumentService;

@RunWith(JUnitParamsRunner.class)
public class WriteFinalDecisionAboutToSubmitHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";
    private WriteFinalDecisionAboutToSubmitHandler handler;

    @Mock
    private Callback<SscsCaseData> callback;

    @Mock
    private CaseDetails<SscsCaseData> caseDetails;

    private PipDecisionNoticeQuestionService pipDecisionNoticeQuestionService;
    private PreviewDocumentService previewDocumentService;
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() throws IOException {
        openMocks(this);
        pipDecisionNoticeQuestionService = new PipDecisionNoticeQuestionService();
        previewDocumentService = new PreviewDocumentService();
        handler = new WriteFinalDecisionAboutToSubmitHandler(pipDecisionNoticeQuestionService, previewDocumentService);

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
    public void givenMobilityStandardRateCorrectAndDailyLivingStandardRateSelectedAndDailyLivingPointsAreTooLow_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
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

    /**
     * Due to a CCD bug ( https://tools.hmcts.net/jira/browse/RDM-8200 ) we have had
     * to implement a workaround in WriteFinalDecisionAboutToSubmitHandler to set
     * the generated date to now, even though it is already being determined by the
     * preview document handler.  This is because on submission, the correct generated date
     * (the one referenced in the preview document) is being overwritten to a null value.
     * Once RDM-8200 is fixed and we remove the workaround, this test should be changed
     * to assert that a "something has gone wrong" error is displayed, as a null generated
     * date would indicate that the date in the preview document hasn't been set.
     *
     */
    @Test
    public void givenValidSubmissionWithGeneratedDateNotSet_thenSetGeneratedDateAsNowAndDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
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

        assertEquals(LocalDate.now().toString(), sscsCaseData.getWriteFinalDecisionGeneratedDate());

    }

    /**
     * This test asserts that whatever the value of the existing generated date from CCD
     * submitted as part of the payload to the WriterFinalSubmissionAboutToSubmitHandler,
     * then that date is updated to now() after the WriterFinalSubmissionAboutToSubmitHandler is called.
     * This is due to a workaround we have implemented in the WriterFinalSubmissionAboutToSubmitHandler
     *
     */
    @Test
    public void givenValidSubmissionWithGeneratedDateSet_thenSetUpdateGeneratedDateAndDoNotDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("standardRate");
        sscsCaseData.setPipWriteFinalDecisionMobilityQuestion("standardRate");
        sscsCaseData.setWriteFinalDecisionGeneratedDate("2018-01-01");

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

        assertEquals(LocalDate.now().toString(), sscsCaseData.getWriteFinalDecisionGeneratedDate());
    }

    @Test
    public void givenMobilityStandardRateCorrectAndDailyLivingEnhancedRateSelectedAndDailyLivingPointsAreTooLow_thenDisplayAnError() {

        when(caseDetails.getCaseData()).thenReturn(sscsCaseData);

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
        sscsCaseData.setPipWriteFinalDecisionDailyLivingQuestion("standardRate");

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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
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

        sscsCaseData.setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");
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
    public void givenDraftFinalDecisionAlreadyExistsOnCase_thenOverwriteExistingDraft() {
        SscsDocument doc = SscsDocument.builder().value(SscsDocumentDetails.builder().documentFileName("oldDraft.doc").documentType(DRAFT_DECISION_NOTICE.getValue()).build()).build();
        List<SscsDocument> docs = new ArrayList<>();
        docs.add(doc);
        callback.getCaseDetails().getCaseData().setSscsDocument(docs);

        callback.getCaseDetails().getCaseData().setWriteFinalDecisionIsDescriptorFlow("yes");
        sscsCaseData.setWriteFinalDecisionGenerateNotice("yes");

        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpDailyLivingQuestion(null);
        callback.getCaseDetails().getCaseData().setPipWriteFinalDecisionComparedToDwpMobilityQuestion(null);
        PreSubmitCallbackResponse<SscsCaseData> response = handler.handle(ABOUT_TO_SUBMIT, callback, USER_AUTHORISATION);

        assertEquals(1, response.getData().getSscsDocument().size());
        assertEquals((String.format("Draft Decision Notice generated on %s.pdf", LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-YYYY")))), response.getData().getSscsDocument().get(0).getValue().getDocumentFileName());
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
