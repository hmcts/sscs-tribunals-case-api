package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.function.Function;
import junitparams.JUnitParamsRunner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsPipCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityQuestion;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipActivityQuestion;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipActivityType;

@RunWith(JUnitParamsRunner.class)
public class PipActivityQuestionTest {

    @Mock
    private SscsPipCaseData sscsPipCaseData;

    @BeforeEach
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void testGetPreparingFoodQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionPreparingFoodQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("preparingFood");
        Assertions.assertNotNull(activityQuestion);
        Assertions.assertNotNull(activityQuestion.getActivityType());
        Assertions.assertNotNull(activityQuestion.getKey());
        Assertions.assertEquals("preparingFood", activityQuestion.getKey());
        Assertions.assertNotNull(activityQuestion.getValue());
        Assertions.assertEquals("Preparing food", activityQuestion.getValue());
        Assertions.assertEquals(PipActivityType.DAILY_LIVING, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assertions.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetTakingNutritionQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionTakingNutritionQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("takingNutrition");
        Assertions.assertNotNull(activityQuestion);
        Assertions.assertNotNull(activityQuestion.getActivityType());
        Assertions.assertNotNull(activityQuestion.getKey());
        Assertions.assertEquals("takingNutrition", activityQuestion.getKey());
        Assertions.assertNotNull(activityQuestion.getValue());
        Assertions.assertEquals("Taking nutrition", activityQuestion.getValue());
        Assertions.assertEquals(PipActivityType.DAILY_LIVING, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assertions.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetManagingTherapyQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionManagingTherapyQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("managingTherapy");
        Assertions.assertNotNull(activityQuestion);
        Assertions.assertNotNull(activityQuestion.getActivityType());
        Assertions.assertNotNull(activityQuestion.getKey());
        Assertions.assertEquals("managingTherapy", activityQuestion.getKey());
        Assertions.assertNotNull(activityQuestion.getValue());
        Assertions.assertEquals("Managing therapy or monitoring a health condition", activityQuestion.getValue());
        Assertions.assertEquals(PipActivityType.DAILY_LIVING, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assertions.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetWashingAndBathingQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionWashAndBatheQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("washingAndBathing");
        Assertions.assertNotNull(activityQuestion);
        Assertions.assertNotNull(activityQuestion.getActivityType());
        Assertions.assertNotNull(activityQuestion.getKey());
        Assertions.assertEquals("washingAndBathing", activityQuestion.getKey());
        Assertions.assertNotNull(activityQuestion.getValue());
        Assertions.assertEquals("Washing and bathing", activityQuestion.getValue());
        Assertions.assertEquals(PipActivityType.DAILY_LIVING, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assertions.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetManagingToiletNeedsQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionManagingToiletNeedsQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("managingToiletNeeds");
        Assertions.assertNotNull(activityQuestion);
        Assertions.assertNotNull(activityQuestion.getActivityType());
        Assertions.assertNotNull(activityQuestion.getKey());
        Assertions.assertEquals("managingToiletNeeds", activityQuestion.getKey());
        Assertions.assertNotNull(activityQuestion.getValue());
        Assertions.assertEquals("Managing toilet needs or incontinence", activityQuestion.getValue());
        Assertions.assertEquals(PipActivityType.DAILY_LIVING, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assertions.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetDressingAndUndressingQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionDressingAndUndressingQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("dressingAndUndressing");
        Assertions.assertNotNull(activityQuestion);
        Assertions.assertNotNull(activityQuestion.getActivityType());
        Assertions.assertNotNull(activityQuestion.getKey());
        Assertions.assertEquals("dressingAndUndressing", activityQuestion.getKey());
        Assertions.assertNotNull(activityQuestion.getValue());
        Assertions.assertEquals("Dressing and undressing", activityQuestion.getValue());
        Assertions.assertEquals(PipActivityType.DAILY_LIVING, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assertions.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetCommunicatingQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionCommunicatingQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("communicating");
        Assertions.assertNotNull(activityQuestion);
        Assertions.assertNotNull(activityQuestion.getActivityType());
        Assertions.assertNotNull(activityQuestion.getKey());
        Assertions.assertEquals("communicating", activityQuestion.getKey());
        Assertions.assertNotNull(activityQuestion.getValue());
        Assertions.assertEquals("Communicating", activityQuestion.getValue());
        Assertions.assertEquals(PipActivityType.DAILY_LIVING, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assertions.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetReadingAndUnderstandingQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionReadingUnderstandingQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("readingUnderstanding");
        Assertions.assertNotNull(activityQuestion);
        Assertions.assertNotNull(activityQuestion.getActivityType());
        Assertions.assertNotNull(activityQuestion.getKey());
        Assertions.assertEquals("readingUnderstanding", activityQuestion.getKey());
        Assertions.assertNotNull(activityQuestion.getValue());
        Assertions.assertEquals("Reading and understanding signs, symbols and words", activityQuestion.getValue());
        Assertions.assertEquals(PipActivityType.DAILY_LIVING, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assertions.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetEngagingWithOthersQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionEngagingWithOthersQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("engagingWithOthers");
        Assertions.assertNotNull(activityQuestion);
        Assertions.assertNotNull(activityQuestion.getActivityType());
        Assertions.assertNotNull(activityQuestion.getKey());
        Assertions.assertEquals("engagingWithOthers", activityQuestion.getKey());
        Assertions.assertNotNull(activityQuestion.getValue());
        Assertions.assertEquals("Engaging with other people face to face", activityQuestion.getValue());
        Assertions.assertEquals(PipActivityType.DAILY_LIVING, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assertions.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetMakingBudgetingDecisionsQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionBudgetingDecisionsQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("budgetingDecisions");
        Assertions.assertNotNull(activityQuestion);
        Assertions.assertNotNull(activityQuestion.getActivityType());
        Assertions.assertNotNull(activityQuestion.getKey());
        Assertions.assertEquals("budgetingDecisions", activityQuestion.getKey());
        Assertions.assertNotNull(activityQuestion.getValue());
        Assertions.assertEquals("Making budgeting decisions", activityQuestion.getValue());
        Assertions.assertEquals(PipActivityType.DAILY_LIVING, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assertions.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetPlanningAndFollowingJourneysQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionPlanningAndFollowingQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("planningAndFollowing");
        Assertions.assertNotNull(activityQuestion);
        Assertions.assertNotNull(activityQuestion.getActivityType());
        Assertions.assertNotNull(activityQuestion.getKey());
        Assertions.assertEquals("planningAndFollowing", activityQuestion.getKey());
        Assertions.assertNotNull(activityQuestion.getValue());
        Assertions.assertEquals("Planning and following journeys", activityQuestion.getValue());
        Assertions.assertEquals(PipActivityType.MOBILITY, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assertions.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetMovingAroundQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionMovingAroundQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("movingAround");
        Assertions.assertNotNull(activityQuestion);
        Assertions.assertNotNull(activityQuestion.getActivityType());
        Assertions.assertNotNull(activityQuestion.getKey());
        Assertions.assertEquals("movingAround", activityQuestion.getKey());
        Assertions.assertNotNull(activityQuestion.getValue());
        Assertions.assertEquals("Moving around", activityQuestion.getValue());
        Assertions.assertEquals(PipActivityType.MOBILITY, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assertions.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetByInvalidQuestionKey() {
        assertThrows(IllegalArgumentException.class, () ->
            PipActivityQuestion.getByKey("invalidQuestion"));
    }

    @Test
    public void testGetByNullQuestionKey() {
        assertThrows(IllegalArgumentException.class, () ->
            PipActivityQuestion.getByKey(null));
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullAnswerExtractors() {
        for (ActivityQuestion activityQuestion : PipActivityQuestion.values()) {
            Assertions.assertNotNull(activityQuestion.getAnswerExtractor());
        }
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullKeys() {
        for (ActivityQuestion activityQuestion : PipActivityQuestion.values()) {
            Assertions.assertNotNull(activityQuestion.getKey());
        }
    }
}
