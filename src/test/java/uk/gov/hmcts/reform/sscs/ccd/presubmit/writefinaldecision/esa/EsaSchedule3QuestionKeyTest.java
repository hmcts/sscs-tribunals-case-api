package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

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
public class EsaSchedule3QuestionKeyTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {

        openMocks(this);
    }

    @Test
    public void testGetMobilisingUnaidedQuestion() {
        Mockito.when(sscsCaseData.getSscsEsaCaseData().getSchedule3Selections()).thenReturn(
            Arrays.asList("schedule3MobilisingUnaided"));
        EsaSchedule3QuestionKey activityQuestion = EsaSchedule3QuestionKey.getByKey("schedule3MobilisingUnaided");
        Assert.assertNotNull(activityQuestion);
        Assert.assertNotNull(activityQuestion.getActivityType());
        Assert.assertNotNull(activityQuestion.getKey());
        Assert.assertEquals("schedule3MobilisingUnaided", activityQuestion.getKey());
        Assert.assertEquals(EsaActivityType.PHYSICAL_DISABILITIES, activityQuestion.getActivityType());
        Function<SscsCaseData, Boolean> answerExtractor = activityQuestion.getAnswerExtractor();
        Assert.assertEquals(Boolean.TRUE, answerExtractor.apply(sscsCaseData));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetByInvalidQuestionKey() {
        EsaSchedule3QuestionKey.getByKey("invalidQuestion");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetByNullQuestionKey() {
        EsaSchedule3QuestionKey.getByKey(null);
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullAnswerExtractors() {
        for (EsaSchedule3QuestionKey activityQuestion : EsaSchedule3QuestionKey.values()) {
            Assert.assertNotNull(activityQuestion.getAnswerExtractor());
        }
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullKeys() {
        for (EsaSchedule3QuestionKey activityQuestion : EsaSchedule3QuestionKey.values()) {
            Assert.assertNotNull(activityQuestion.getKey());
        }
    }
}
