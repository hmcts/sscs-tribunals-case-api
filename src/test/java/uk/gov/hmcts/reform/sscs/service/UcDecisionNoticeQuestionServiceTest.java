package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityAnswer;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcActivityQuestion;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcActivityQuestionKey;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcActivityType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcSchedule7QuestionKey;

public class UcDecisionNoticeQuestionServiceTest extends DecisionNoticeQuestionServiceTestBase<UcDecisionNoticeQuestionService> {

    @Override
    protected UcDecisionNoticeQuestionService createDecisionNoticeQuestionService() throws IOException {
        return new UcDecisionNoticeQuestionService();
    }

    @Test
    public void givenASelectedQuestionKey_thenExtractTheQuestionFromTheText() {
        UcActivityQuestion question = service.extractQuestionFromKey(UcActivityQuestionKey.MOBILISING_UNAIDED);
        Assert.assertNotNull(question);
        Assert.assertEquals("mobilisingUnaided", question.getKey());
        assertEquals("1. Mobilising unaided by another person with or without a walking stick, manual wheelchair or other aid if such aid is normally or could reasonably be worn or used.", question.getValue());
        assertEquals(UcActivityType.PHYSICAL_DISABILITIES, question.getActivityType());
    }

    @Test
    public void givenASchedule7QuestionKey_thenExtractTheAnswerFromTheText() {

        SscsCaseData sscsCaseData = SscsCaseData.builder().build();

        Optional<ActivityAnswer> answerOptional = service.getAnswerForActivityQuestionKey(sscsCaseData, UcSchedule7QuestionKey.MOBILISING_UNAIDED.getKey());
        Assert.assertNotNull(answerOptional);
        Assert.assertTrue(answerOptional.isPresent());

        Assert.assertEquals("1", answerOptional.get().getActivityAnswerNumber());
        Assert.assertNull(answerOptional.get().getActivityAnswerLetter());
        Assert.assertNull(answerOptional.get().getActivityAnswerValue());
        Assert.assertEquals(0, answerOptional.get().getActivityAnswerPoints());
    }

    @Test
    public void givenAllSchedule7QuestionKeys_thenExtractTheAnswerFromTheText() {

        SscsCaseData sscsCaseData = SscsCaseData.builder().build();

        for (UcSchedule7QuestionKey key :  UcSchedule7QuestionKey.values()) {
            Optional<ActivityAnswer> answerOptional = service.getAnswerForActivityQuestionKey(sscsCaseData, key.getKey());
            Assert.assertNotNull(answerOptional);
            Assert.assertTrue(answerOptional.isPresent());
            Assert.assertNotNull(answerOptional.get().getActivityAnswerNumber());
            Assert.assertNull(answerOptional.get().getActivityAnswerLetter());
            Assert.assertNull(answerOptional.get().getActivityAnswerValue());
            Assert.assertEquals(0, answerOptional.get().getActivityAnswerPoints());
        }


    }
}
