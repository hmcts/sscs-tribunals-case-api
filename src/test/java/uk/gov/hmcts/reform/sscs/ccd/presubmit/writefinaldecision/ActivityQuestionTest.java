package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.mockito.MockitoAnnotations.initMocks;

import java.util.function.Function;
import junitparams.JUnitParamsRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class ActivityQuestionTest {

    @Mock
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        initMocks(this);
    }

    @Test
    public void testGetPreparingFoodQuestion() {
        Mockito.when(sscsCaseData.getPipWriteFinalDecisionPreparingFoodQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = ActivityQuestion.getByKey("preparingFood");
        Assert.assertNotNull(activityQuestion);
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetTakingNutritionQuestion() {
        Mockito.when(sscsCaseData.getPipWriteFinalDecisionTakingNutritionQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = ActivityQuestion.getByKey("takingNutrition");
        Assert.assertNotNull(activityQuestion);
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetManagingTherapyQuestion() {
        Mockito.when(sscsCaseData.getPipWriteFinalDecisionManagingTherapyQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = ActivityQuestion.getByKey("managingTherapy");
        Assert.assertNotNull(activityQuestion);
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetWashingAndBathingQuestion() {
        Mockito.when(sscsCaseData.getPipWriteFinalDecisionWashAndBatheQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = ActivityQuestion.getByKey("washingAndBathing");
        Assert.assertNotNull(activityQuestion);
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetManagingToiletNeedsQuestion() {
        Mockito.when(sscsCaseData.getPipWriteFinalDecisionManagingToiletNeedsQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = ActivityQuestion.getByKey("managingToiletNeeds");
        Assert.assertNotNull(activityQuestion);
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetDressingAndUndressingQuestion() {
        Mockito.when(sscsCaseData.getPipWriteFinalDecisionDressingAndUndressingQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = ActivityQuestion.getByKey("dressingAndUndressing");
        Assert.assertNotNull(activityQuestion);
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetCommunicatingQuestion() {
        Mockito.when(sscsCaseData.getPipWriteFinalDecisionCommunicatingQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = ActivityQuestion.getByKey("communicating");
        Assert.assertNotNull(activityQuestion);
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetReadingAndUnderstandingQuestion() {
        Mockito.when(sscsCaseData.getPipWriteFinalDecisionReadingUnderstandingQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = ActivityQuestion.getByKey("readingUnderstanding");
        Assert.assertNotNull(activityQuestion);
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetEngagingWithOthersQuestion() {
        Mockito.when(sscsCaseData.getPipWriteFinalDecisionEngagingWithOthersQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = ActivityQuestion.getByKey("engagingWithOthers");
        Assert.assertNotNull(activityQuestion);
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetMakingBudgetingDecisionsQuestion() {
        Mockito.when(sscsCaseData.getPipWriteFinalDecisionBudgetingDecisionsQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = ActivityQuestion.getByKey("budgetingDecisions");
        Assert.assertNotNull(activityQuestion);
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetPlanningAndFollowingJourneysQuestion() {
        Mockito.when(sscsCaseData.getPipWriteFinalDecisionPlanningAndFollowingQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = ActivityQuestion.getByKey("planningAndFollowing");
        Assert.assertNotNull(activityQuestion);
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetMovingAroundQuestion() {
        Mockito.when(sscsCaseData.getPipWriteFinalDecisionMovingAroundQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = ActivityQuestion.getByKey("movingAround");
        Assert.assertNotNull(activityQuestion);
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetByInvalidQuestionKey() {
        ActivityQuestion.getByKey("invalidQuestion");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetByNullQuestionKey() {
        ActivityQuestion.getByKey(null);
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullAnswerExtractors() {
        for (ActivityQuestion activityQuestion : ActivityQuestion.values()) {
            Assert.assertNotNull(activityQuestion.getAnswerExtractor());
        }
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullKeys() {
        for (ActivityQuestion activityQuestion : ActivityQuestion.values()) {
            Assert.assertNotNull(activityQuestion.key);
        }
    }
}
