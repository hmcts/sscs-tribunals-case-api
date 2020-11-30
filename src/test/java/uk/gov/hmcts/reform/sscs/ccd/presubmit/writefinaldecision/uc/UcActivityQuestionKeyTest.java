package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsUcCaseData;

@RunWith(JUnitParamsRunner.class)
public class UcActivityQuestionKeyTest {

    @Mock
    private SscsUcCaseData sscsUcCaseData;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void testGetMobilisingUnaidedQuestion() {
        final SscsCaseData sscsCaseData = SscsCaseData.builder().sscsUcCaseData(sscsUcCaseData).build();
        Mockito.when(sscsUcCaseData.getUcWriteFinalDecisionMobilisingUnaidedQuestion()).thenReturn("testAnswer");
        UcActivityQuestionKey activityQuestion = UcActivityQuestionKey.getByKey("mobilisingUnaided");
        Assert.assertNotNull(activityQuestion);
        Assert.assertNotNull(activityQuestion.getActivityType());
        Assert.assertNotNull(activityQuestion.getKey());
        Assert.assertEquals("mobilisingUnaided", activityQuestion.getKey());
        Assert.assertEquals(UcActivityType.PHYSICAL_DISABILITIES, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetByInvalidQuestionKey() {
        UcActivityQuestionKey.getByKey("invalidQuestion");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetByNullQuestionKey() {
        UcActivityQuestionKey.getByKey(null);
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullAnswerExtractors() {
        for (UcActivityQuestionKey activityQuestion : UcActivityQuestionKey.values()) {
            Assert.assertNotNull(activityQuestion.getAnswerExtractor());
        }
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullKeys() {
        for (UcActivityQuestionKey activityQuestion : UcActivityQuestionKey.values()) {
            Assert.assertNotNull(activityQuestion.getKey());
        }
    }
}
