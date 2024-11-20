package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsEsaCaseData;

@RunWith(JUnitParamsRunner.class)
public class EsaActivityQuestionKeyTest {

    @Mock
    private SscsEsaCaseData sscsEsaCaseData;

    @BeforeEach
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void testGetMobilisingUnaidedQuestion() {
        final SscsCaseData sscsCaseData = SscsCaseData.builder().sscsEsaCaseData(sscsEsaCaseData).build();
        Mockito.when(sscsEsaCaseData.getEsaWriteFinalDecisionMobilisingUnaidedQuestion()).thenReturn("testAnswer");
        EsaActivityQuestionKey activityQuestion = EsaActivityQuestionKey.getByKey("mobilisingUnaided");
        Assertions.assertNotNull(activityQuestion);
        Assertions.assertNotNull(activityQuestion.getActivityType());
        Assertions.assertNotNull(activityQuestion.getKey());
        Assertions.assertEquals("mobilisingUnaided", activityQuestion.getKey());
        Assertions.assertEquals(EsaActivityType.PHYSICAL_DISABILITIES, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        Assertions.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetByInvalidQuestionKey() {
        assertThrows(IllegalArgumentException.class, () ->
            EsaActivityQuestionKey.getByKey("invalidQuestion"));
    }

    @Test
    public void testGetByNullQuestionKey() {
        assertThrows(IllegalArgumentException.class, () ->
            EsaActivityQuestionKey.getByKey(null));
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullAnswerExtractors() {
        for (EsaActivityQuestionKey activityQuestion : EsaActivityQuestionKey.values()) {
            Assertions.assertNotNull(activityQuestion.getAnswerExtractor());
        }
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullKeys() {
        for (EsaActivityQuestionKey activityQuestion : EsaActivityQuestionKey.values()) {
            Assertions.assertNotNull(activityQuestion.getKey());
        }
    }
}
