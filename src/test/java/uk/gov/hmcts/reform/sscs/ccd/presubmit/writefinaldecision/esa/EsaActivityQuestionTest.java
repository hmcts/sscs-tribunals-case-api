package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

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
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityQuestion;

@RunWith(JUnitParamsRunner.class)
public class EsaActivityQuestionTest {

    @Mock
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void testGetMobilisingUnaidedQuestion() {
        Mockito.when(sscsCaseData.getEsaWriteFinalDecisionMobilisingUnaidedQuestion()).thenReturn("testAnswer");
        ActivityQuestion activityQuestion = EsaActivityQuestion.getByKey("mobilisingUnaided");
        Assert.assertNotNull(activityQuestion);
        Assert.assertNotNull(activityQuestion.getActivityType());
        Assert.assertNotNull(activityQuestion.getKey());
        Assert.assertEquals("mobilisingUnaided", activityQuestion.getKey());
        Assert.assertNotNull(activityQuestion.getValue());
        Assert.assertEquals("Mobilising Unaided", activityQuestion.getValue());
        Assert.assertEquals(EsaActivityType.PHYSICAL_DISABLITIES, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetByInvalidQuestionKey() {
        EsaActivityQuestion.getByKey("invalidQuestion");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetByNullQuestionKey() {
        EsaActivityQuestion.getByKey(null);
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullAnswerExtractors() {
        for (ActivityQuestion activityQuestion : EsaActivityQuestion.values()) {
            Assert.assertNotNull(activityQuestion.getAnswerExtractor());
        }
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullKeys() {
        for (ActivityQuestion activityQuestion : EsaActivityQuestion.values()) {
            Assert.assertNotNull(activityQuestion.getKey());
        }
    }
}
