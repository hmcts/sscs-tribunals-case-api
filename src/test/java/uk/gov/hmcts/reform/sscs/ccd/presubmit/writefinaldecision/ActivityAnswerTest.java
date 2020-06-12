package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


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

        assertEquals("ActivityAnswer(activityAnswerNumber=1, "
            + "activityAnswerLetter=answerLetter, activityAnswerValue=answerValue, activityAnswerPoints=10)", activityAnswer.toString());

    }

    @Test
    public void testActivityAnswerConstructor() {
        ActivityAnswer activityAnswer = new ActivityAnswer(
            "1",
            "answerLetter",
            "answerValue",
            10);

        assertEquals("answerValue", activityAnswer.getActivityAnswerValue());
        assertEquals("answerLetter", activityAnswer.getActivityAnswerLetter());
        assertEquals(10, activityAnswer.getActivityAnswerPoints());
        assertEquals("1", activityAnswer.getActivityAnswerNumber());

        assertEquals("ActivityAnswer(activityAnswerNumber=1, "
            + "activityAnswerLetter=answerLetter, activityAnswerValue=answerValue, activityAnswerPoints=10)", activityAnswer.toString());

    }

    @Test
    public void testActivityAnswerHashcodeAndEquals() {
        ActivityAnswer activityAnswer1 = new ActivityAnswer(
            "1",
            "answerLetter",
            "answerValue",
            10);

        ActivityAnswer activityAnswer2 = new ActivityAnswer(
            "1",
            "answerLetter",
            "answerValue",
            10);


        ActivityAnswer activityAnswer3 = new ActivityAnswer(
            "2",
            "answerLetter",
            "answerValue",
            10);

        assertEquals(activityAnswer1, activityAnswer2);
        assertEquals(activityAnswer1.hashCode(), activityAnswer2.hashCode());
        assertNotEquals(activityAnswer1, activityAnswer3);
        assertNotEquals(activityAnswer1.hashCode(), activityAnswer3.hashCode());

    }

}
