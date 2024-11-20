package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Arrays;
import java.util.function.Function;
import junitparams.JUnitParamsRunner;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class EsaSchedule3QuestionKeyTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SscsCaseData sscsCaseData;

    @BeforeEach
    public void setUp() {

        openMocks(this);
    }

    @Test
    public void testGetMobilisingUnaidedQuestion() {
        Mockito.when(sscsCaseData.getSscsEsaCaseData().getSchedule3Selections()).thenReturn(
            Arrays.asList("schedule3MobilisingUnaided"));
        EsaSchedule3QuestionKey activityQuestion = EsaSchedule3QuestionKey.getByKey("schedule3MobilisingUnaided");
        Assertions.assertNotNull(activityQuestion);
        Assertions.assertNotNull(activityQuestion.getActivityType());
        Assertions.assertNotNull(activityQuestion.getKey());
        Assertions.assertEquals("schedule3MobilisingUnaided", activityQuestion.getKey());
        Assertions.assertEquals(EsaActivityType.PHYSICAL_DISABILITIES, activityQuestion.getActivityType());
        Function<SscsCaseData, Boolean> answerExtractor = activityQuestion.getAnswerExtractor();
        Assertions.assertEquals(Boolean.TRUE, answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetByInvalidQuestionKey() {
        assertThrows(IllegalArgumentException.class, () ->
            EsaSchedule3QuestionKey.getByKey("invalidQuestion"));
    }

    @Test
    public void testGetByNullQuestionKey() {
        assertThrows(IllegalArgumentException.class, () ->
            EsaSchedule3QuestionKey.getByKey(null));
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullAnswerExtractors() {
        for (EsaSchedule3QuestionKey activityQuestion : EsaSchedule3QuestionKey.values()) {
            Assertions.assertNotNull(activityQuestion.getAnswerExtractor());
        }
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullKeys() {
        for (EsaSchedule3QuestionKey activityQuestion : EsaSchedule3QuestionKey.values()) {
            Assertions.assertNotNull(activityQuestion.getKey());
        }
    }
}
