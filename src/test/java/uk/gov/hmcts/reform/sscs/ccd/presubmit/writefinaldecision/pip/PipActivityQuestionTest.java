package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import static org.mockito.MockitoAnnotations.openMocks;

import java.util.function.Function;
import junitparams.JUnitParamsRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
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

    @Before
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void testGetPreparingFoodQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionPreparingFoodQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("preparingFood");
        Assert.assertNotNull(activityQuestion);
        Assert.assertNotNull(activityQuestion.getActivityType());
        Assert.assertNotNull(activityQuestion.getKey());
        Assert.assertEquals("preparingFood", activityQuestion.getKey());
        Assert.assertNotNull(activityQuestion.getValue());
        Assert.assertEquals("Preparing food", activityQuestion.getValue());
        Assert.assertEquals(PipActivityType.DAILY_LIVING, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetTakingNutritionQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionTakingNutritionQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("takingNutrition");
        Assert.assertNotNull(activityQuestion);
        Assert.assertNotNull(activityQuestion.getActivityType());
        Assert.assertNotNull(activityQuestion.getKey());
        Assert.assertEquals("takingNutrition", activityQuestion.getKey());
        Assert.assertNotNull(activityQuestion.getValue());
        Assert.assertEquals("Taking nutrition", activityQuestion.getValue());
        Assert.assertEquals(PipActivityType.DAILY_LIVING, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetManagingTherapyQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionManagingTherapyQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("managingTherapy");
        Assert.assertNotNull(activityQuestion);
        Assert.assertNotNull(activityQuestion.getActivityType());
        Assert.assertNotNull(activityQuestion.getKey());
        Assert.assertEquals("managingTherapy", activityQuestion.getKey());
        Assert.assertNotNull(activityQuestion.getValue());
        Assert.assertEquals("Managing therapy or monitoring a health condition", activityQuestion.getValue());
        Assert.assertEquals(PipActivityType.DAILY_LIVING, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetWashingAndBathingQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionWashAndBatheQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("washingAndBathing");
        Assert.assertNotNull(activityQuestion);
        Assert.assertNotNull(activityQuestion.getActivityType());
        Assert.assertNotNull(activityQuestion.getKey());
        Assert.assertEquals("washingAndBathing", activityQuestion.getKey());
        Assert.assertNotNull(activityQuestion.getValue());
        Assert.assertEquals("Washing and bathing", activityQuestion.getValue());
        Assert.assertEquals(PipActivityType.DAILY_LIVING, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetManagingToiletNeedsQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionManagingToiletNeedsQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("managingToiletNeeds");
        Assert.assertNotNull(activityQuestion);
        Assert.assertNotNull(activityQuestion.getActivityType());
        Assert.assertNotNull(activityQuestion.getKey());
        Assert.assertEquals("managingToiletNeeds", activityQuestion.getKey());
        Assert.assertNotNull(activityQuestion.getValue());
        Assert.assertEquals("Managing toilet needs or incontinence", activityQuestion.getValue());
        Assert.assertEquals(PipActivityType.DAILY_LIVING, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetDressingAndUndressingQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionDressingAndUndressingQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("dressingAndUndressing");
        Assert.assertNotNull(activityQuestion);
        Assert.assertNotNull(activityQuestion.getActivityType());
        Assert.assertNotNull(activityQuestion.getKey());
        Assert.assertEquals("dressingAndUndressing", activityQuestion.getKey());
        Assert.assertNotNull(activityQuestion.getValue());
        Assert.assertEquals("Dressing and undressing", activityQuestion.getValue());
        Assert.assertEquals(PipActivityType.DAILY_LIVING, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetCommunicatingQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionCommunicatingQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("communicating");
        Assert.assertNotNull(activityQuestion);
        Assert.assertNotNull(activityQuestion.getActivityType());
        Assert.assertNotNull(activityQuestion.getKey());
        Assert.assertEquals("communicating", activityQuestion.getKey());
        Assert.assertNotNull(activityQuestion.getValue());
        Assert.assertEquals("Communicating", activityQuestion.getValue());
        Assert.assertEquals(PipActivityType.DAILY_LIVING, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetReadingAndUnderstandingQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionReadingUnderstandingQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("readingUnderstanding");
        Assert.assertNotNull(activityQuestion);
        Assert.assertNotNull(activityQuestion.getActivityType());
        Assert.assertNotNull(activityQuestion.getKey());
        Assert.assertEquals("readingUnderstanding", activityQuestion.getKey());
        Assert.assertNotNull(activityQuestion.getValue());
        Assert.assertEquals("Reading and understanding signs, symbols and words", activityQuestion.getValue());
        Assert.assertEquals(PipActivityType.DAILY_LIVING, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetEngagingWithOthersQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionEngagingWithOthersQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("engagingWithOthers");
        Assert.assertNotNull(activityQuestion);
        Assert.assertNotNull(activityQuestion.getActivityType());
        Assert.assertNotNull(activityQuestion.getKey());
        Assert.assertEquals("engagingWithOthers", activityQuestion.getKey());
        Assert.assertNotNull(activityQuestion.getValue());
        Assert.assertEquals("Engaging with other people face to face", activityQuestion.getValue());
        Assert.assertEquals(PipActivityType.DAILY_LIVING, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetMakingBudgetingDecisionsQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionBudgetingDecisionsQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("budgetingDecisions");
        Assert.assertNotNull(activityQuestion);
        Assert.assertNotNull(activityQuestion.getActivityType());
        Assert.assertNotNull(activityQuestion.getKey());
        Assert.assertEquals("budgetingDecisions", activityQuestion.getKey());
        Assert.assertNotNull(activityQuestion.getValue());
        Assert.assertEquals("Making budgeting decisions", activityQuestion.getValue());
        Assert.assertEquals(PipActivityType.DAILY_LIVING, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetPlanningAndFollowingJourneysQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionPlanningAndFollowingQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("planningAndFollowing");
        Assert.assertNotNull(activityQuestion);
        Assert.assertNotNull(activityQuestion.getActivityType());
        Assert.assertNotNull(activityQuestion.getKey());
        Assert.assertEquals("planningAndFollowing", activityQuestion.getKey());
        Assert.assertNotNull(activityQuestion.getValue());
        Assert.assertEquals("Planning and following journeys", activityQuestion.getValue());
        Assert.assertEquals(PipActivityType.MOBILITY, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetMovingAroundQuestion() {
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionMovingAroundQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = PipActivityQuestion.getByKey("movingAround");
        Assert.assertNotNull(activityQuestion);
        Assert.assertNotNull(activityQuestion.getActivityType());
        Assert.assertNotNull(activityQuestion.getKey());
        Assert.assertEquals("movingAround", activityQuestion.getKey());
        Assert.assertNotNull(activityQuestion.getValue());
        Assert.assertEquals("Moving around", activityQuestion.getValue());
        Assert.assertEquals(PipActivityType.MOBILITY, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetByInvalidQuestionKey() {
        PipActivityQuestion.getByKey("invalidQuestion");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetByNullQuestionKey() {
        PipActivityQuestion.getByKey(null);
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullAnswerExtractors() {
        for (ActivityQuestion activityQuestion : PipActivityQuestion.values()) {
            Assert.assertNotNull(activityQuestion.getAnswerExtractor());
        }
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullKeys() {
        for (ActivityQuestion activityQuestion : PipActivityQuestion.values()) {
            Assert.assertNotNull(activityQuestion.getKey());
        }
    }
}
