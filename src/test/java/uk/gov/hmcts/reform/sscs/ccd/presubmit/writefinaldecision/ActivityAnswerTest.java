package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ActivityAnswerTest {

    @Test
    public void testActivityAnswerBuilder() {
        ActivityAnswer activityAnswer = ActivityAnswer.builder()
            .activityAnswerValue("answerValue")
            .activityAnswerLetter("answerLetter")
            .activityAnswerPoints(10)
            .activityAnswerNumber("1")
            .build();

        assertEquals("answerValue", activityAnswer.getActivityAnswerValue());
        assertEquals("answerLetter", activityAnswer.getActivityAnswerLetter());
        assertEquals(10, activityAnswer.getActivityAnswerPoints());
        assertEquals("1", activityAnswer.getActivityAnswerNumber());
    }

}
