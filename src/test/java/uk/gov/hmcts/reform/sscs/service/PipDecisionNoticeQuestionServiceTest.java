package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityAnswer;

public class PipDecisionNoticeQuestionServiceTest extends DecisionNoticeQuestionServiceTestBase<PipDecisionNoticeQuestionService> {

    @Override
    protected PipDecisionNoticeQuestionService createDecisionNoticeQuestionService() throws IOException {
        return new PipDecisionNoticeQuestionService();
    }

    @Test
    public void givenASelectedAnswerForADecisionNoticeQuestion_thenExtractTheAnswerFromTheText() {
        Optional<ActivityAnswer> answer = service.extractAnswerFromSelectedValue("preparingFood1f");
        Assert.assertNotNull(answer);
        Assert.assertTrue(answer.isPresent());
        assertEquals(8, answer.get().getActivityAnswerPoints());
        assertEquals("1", answer.get().getActivityAnswerNumber());
        assertEquals("f", answer.get().getActivityAnswerLetter());
        assertEquals("Cannot prepare and cook food.", answer.get().getActivityAnswerValue());
    }
}