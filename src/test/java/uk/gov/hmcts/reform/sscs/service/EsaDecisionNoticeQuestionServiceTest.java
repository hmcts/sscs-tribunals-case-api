package uk.gov.hmcts.reform.sscs.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityAnswer;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaActivityQuestion;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaActivityQuestionKey;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaActivityType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaSchedule3QuestionKey;

public class EsaDecisionNoticeQuestionServiceTest extends DecisionNoticeQuestionServiceTestBase<EsaDecisionNoticeQuestionService> {

    @Override
    protected EsaDecisionNoticeQuestionService createDecisionNoticeQuestionService() throws IOException {
        return new EsaDecisionNoticeQuestionService();
    }

    @Test
    public void givenASelectedQuestionKey_thenExtractTheQuestionFromTheText() {
        EsaActivityQuestion question = service.extractQuestionFromKey(EsaActivityQuestionKey.MOBILISING_UNAIDED);
        Assertions.assertNotNull(question);
        Assertions.assertEquals("mobilisingUnaided", question.getKey());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", question.getValue());
        assertEquals(EsaActivityType.PHYSICAL_DISABILITIES, question.getActivityType());
    }

    @Test
    public void givenASchedule3QuestionKey_thenExtractTheAnswerFromTheText() {

        SscsCaseData sscsCaseData = SscsCaseData.builder().build();

        Optional<ActivityAnswer> answerOptional = service.getAnswerForActivityQuestionKey(sscsCaseData, EsaSchedule3QuestionKey.MOBILISING_UNAIDED.getKey());
        Assertions.assertNotNull(answerOptional);
        Assertions.assertTrue(answerOptional.isPresent());

        Assertions.assertEquals("1", answerOptional.get().getActivityAnswerNumber());
        Assertions.assertNull(answerOptional.get().getActivityAnswerLetter());
        Assertions.assertNull(answerOptional.get().getActivityAnswerValue());
        Assertions.assertEquals(0, answerOptional.get().getActivityAnswerPoints());
    }

    @Test
    public void givenAllSchedule3QuestionKeys_thenExtractTheAnswerFromTheText() {

        SscsCaseData sscsCaseData = SscsCaseData.builder().build();

        for (EsaSchedule3QuestionKey key :  EsaSchedule3QuestionKey.values()) {
            Optional<ActivityAnswer> answerOptional = service.getAnswerForActivityQuestionKey(sscsCaseData, key.getKey());
            Assertions.assertNotNull(answerOptional);
            Assertions.assertTrue(answerOptional.isPresent());
            Assertions.assertNotNull(answerOptional.get().getActivityAnswerNumber());
            Assertions.assertNull(answerOptional.get().getActivityAnswerLetter());
            Assertions.assertNull(answerOptional.get().getActivityAnswerValue());
            Assertions.assertEquals(0, answerOptional.get().getActivityAnswerPoints());
        }


    }
}
