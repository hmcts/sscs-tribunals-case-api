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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsEsaCaseData;

@RunWith(JUnitParamsRunner.class)
public class EsaActivityQuestionKeyTest {

    @Mock
    private SscsEsaCaseData sscsEsaCaseData;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void testGetMobilisingUnaidedQuestion() {
        final SscsCaseData sscsCaseData = SscsCaseData.builder().sscsEsaCaseData(sscsEsaCaseData).build();
        Mockito.when(sscsEsaCaseData.getEsaWriteFinalDecisionMobilisingUnaidedQuestion()).thenReturn("testAnswer");
        EsaActivityQuestionKey activityQuestion = EsaActivityQuestionKey.getByKey("mobilisingUnaided");
        Assert.assertNotNull(activityQuestion);
        Assert.assertNotNull(activityQuestion.getActivityType());
        Assert.assertNotNull(activityQuestion.getKey());
        Assert.assertEquals("mobilisingUnaided", activityQuestion.getKey());
        Assert.assertEquals(EsaActivityType.PHYSICAL_DISABILITIES, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        Assert.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetByInvalidQuestionKey() {
        EsaActivityQuestionKey.getByKey("invalidQuestion");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetByNullQuestionKey() {
        EsaActivityQuestionKey.getByKey(null);
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullAnswerExtractors() {
        for (EsaActivityQuestionKey activityQuestion : EsaActivityQuestionKey.values()) {
            Assert.assertNotNull(activityQuestion.getAnswerExtractor());
        }
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullKeys() {
        for (EsaActivityQuestionKey activityQuestion : EsaActivityQuestionKey.values()) {
            Assert.assertNotNull(activityQuestion.getKey());
        }
    }
}
