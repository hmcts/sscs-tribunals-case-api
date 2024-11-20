package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

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
public class UcSchedule7QuestionKeyTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SscsCaseData sscsCaseData;

    @BeforeEach
    public void setUp() {

        openMocks(this);
    }

    @Test
    public void testGetMobilisingUnaidedQuestion() {
        Mockito.when(sscsCaseData.getSscsUcCaseData().getSchedule7Selections()).thenReturn(
            Arrays.asList("schedule7MobilisingUnaided"));
        UcSchedule7QuestionKey activityQuestion = UcSchedule7QuestionKey.getByKey("schedule7MobilisingUnaided");
        Assertions.assertNotNull(activityQuestion);
        Assertions.assertNotNull(activityQuestion.getActivityType());
        Assertions.assertNotNull(activityQuestion.getKey());
        Assertions.assertEquals("schedule7MobilisingUnaided", activityQuestion.getKey());
        Assertions.assertEquals(UcActivityType.PHYSICAL_DISABILITIES, activityQuestion.getActivityType());
        Function<SscsCaseData, Boolean> answerExtractor = activityQuestion.getAnswerExtractor();
        Assertions.assertEquals(Boolean.TRUE, answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetByInvalidQuestionKey() {
        assertThrows(IllegalArgumentException.class, () ->
            UcSchedule7QuestionKey.getByKey("invalidQuestion"));
    }

    @Test
    public void testGetByNullQuestionKey() {
        assertThrows(IllegalArgumentException.class, () ->
            UcSchedule7QuestionKey.getByKey(null));
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullAnswerExtractors() {
        for (UcSchedule7QuestionKey activityQuestion : UcSchedule7QuestionKey.values()) {
            Assertions.assertNotNull(activityQuestion.getAnswerExtractor());
        }
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullKeys() {
        for (UcSchedule7QuestionKey activityQuestion : UcSchedule7QuestionKey.values()) {
            Assertions.assertNotNull(activityQuestion.getKey());
        }
    }
}
