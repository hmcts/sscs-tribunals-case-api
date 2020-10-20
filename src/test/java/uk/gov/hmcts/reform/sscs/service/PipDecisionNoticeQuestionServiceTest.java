package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityAnswer;

public class PipDecisionNoticeQuestionServiceTest {

    private PipDecisionNoticeQuestionService service;

    @Before
    public void setup() throws IOException {
        service = new PipDecisionNoticeQuestionService();
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

    @Test
    public void givenANonMatchedNumber_thenReturnEmptyAnswer() {
        Optional<ActivityAnswer> answer = service.extractAnswerFromSelectedValue("random");
        Assert.assertNotNull(answer);
        Assert.assertFalse(answer.isPresent());
    }

    @Test
    public void givenANullNumber_thenReturnEmptyAnswer() {
        Optional<ActivityAnswer> answer = service.extractAnswerFromSelectedValue(null);
        Assert.assertNotNull(answer);
        Assert.assertFalse(answer.isPresent());
    }
}