package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityAnswer;

public class EsaDecisionNoticeQuestionServiceTest {

    private EsaDecisionNoticeQuestionService service;

    @Before
    public void setup() throws IOException {
        service = new EsaDecisionNoticeQuestionService();
    }

    @Test
    public void givenASelectedAnswerForADecisionNoticeQuestion_thenExtractTheAnswerFromTheText() {
        Optional<ActivityAnswer> answer = service.extractAnswerFromSelectedValue("mobilisingUnaided1a");
        Assert.assertNotNull(answer);
        Assert.assertTrue(answer.isPresent());
        assertEquals(15, answer.get().getActivityAnswerPoints());
        assertEquals("1", answer.get().getActivityAnswerNumber());
        assertEquals("a", answer.get().getActivityAnswerLetter());
        assertEquals("Cannot, unaided by another person, either: (i) "
            + "mobilise more than 50 metres on level ground without stopping in "
            + "order to avoid significant discomfort or exhaustion; or (ii) repeatedly "
            + "mobilise 50 metres within a reasonable timescale because of significant "
            + "discomfort or exhaustion.", answer.get().getActivityAnswerValue());
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