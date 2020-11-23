package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Arrays;
import java.util.function.Function;
import junitparams.JUnitParamsRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class UcSchedule7QuestionKeyTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {

        openMocks(this);
    }

    @Test
    public void testGetMobilisingUnaidedQuestion() {
        Mockito.when(sscsCaseData.getSscsUcCaseData().getSchedule7Selections()).thenReturn(
            Arrays.asList("schedule7MobilisingUnaided"));
        UcSchedule7QuestionKey activityQuestion = UcSchedule7QuestionKey.getByKey("schedule7MobilisingUnaided");
        Assert.assertNotNull(activityQuestion);
        Assert.assertNotNull(activityQuestion.getActivityType());
        Assert.assertNotNull(activityQuestion.getKey());
        Assert.assertEquals("schedule7MobilisingUnaided", activityQuestion.getKey());
        Assert.assertEquals(UcActivityType.PHYSICAL_DISABILITIES, activityQuestion.getActivityType());
        Function<SscsCaseData, Boolean> answerExtractor = activityQuestion.getAnswerExtractor();
        Assert.assertEquals(Boolean.TRUE, answerExtractor.apply(sscsCaseData));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetByInvalidQuestionKey() {
        UcSchedule7QuestionKey.getByKey("invalidQuestion");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetByNullQuestionKey() {
        UcSchedule7QuestionKey.getByKey(null);
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullAnswerExtractors() {
        for (UcSchedule7QuestionKey activityQuestion : UcSchedule7QuestionKey.values()) {
            Assert.assertNotNull(activityQuestion.getAnswerExtractor());
        }
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullKeys() {
        for (UcSchedule7QuestionKey activityQuestion : UcSchedule7QuestionKey.values()) {
            Assert.assertNotNull(activityQuestion.getKey());
        }
    }
}
