package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

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
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsUcCaseData;

@RunWith(JUnitParamsRunner.class)
public class UcActivityQuestionKeyTest {

    @Mock
    private SscsUcCaseData sscsUcCaseData;

    @BeforeEach
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void testGetMobilisingUnaidedQuestion() {
        final SscsCaseData sscsCaseData = SscsCaseData.builder().sscsUcCaseData(sscsUcCaseData).build();
        Mockito.when(sscsUcCaseData.getUcWriteFinalDecisionMobilisingUnaidedQuestion()).thenReturn("testAnswer");
        UcActivityQuestionKey activityQuestion = UcActivityQuestionKey.getByKey("mobilisingUnaided");
        Assertions.assertNotNull(activityQuestion);
        Assertions.assertNotNull(activityQuestion.getActivityType());
        Assertions.assertNotNull(activityQuestion.getKey());
        Assertions.assertEquals("mobilisingUnaided", activityQuestion.getKey());
        Assertions.assertEquals(UcActivityType.PHYSICAL_DISABILITIES, activityQuestion.getActivityType());
        Function<SscsCaseData, String> answerExtractor = activityQuestion.getAnswerExtractor();
        Assertions.assertEquals("testAnswer", answerExtractor.apply(sscsCaseData));
    }

    @Test
    public void testGetByInvalidQuestionKey() {
        assertThrows(IllegalArgumentException.class, () ->
            UcActivityQuestionKey.getByKey("invalidQuestion"));
    }

    @Test
    public void testGetByNullQuestionKey() {
        assertThrows(IllegalArgumentException.class, () ->
            UcActivityQuestionKey.getByKey(null));
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullAnswerExtractors() {
        for (UcActivityQuestionKey activityQuestion : UcActivityQuestionKey.values()) {
            Assertions.assertNotNull(activityQuestion.getAnswerExtractor());
        }
    }

    @Test
    public void testAllActivityQuestionsHaveNonNullKeys() {
        for (UcActivityQuestionKey activityQuestion : UcActivityQuestionKey.values()) {
            Assertions.assertNotNull(activityQuestion.getKey());
        }
    }
}
