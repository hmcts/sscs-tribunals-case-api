package uk.gov.hmcts.reform.sscs.model.docassembly;

import static org.junit.Assert.*;

import org.junit.Test;

public class DescriptorTest {

    @Test
    public void testDescriptorBuilder() {
        Descriptor descriptor = Descriptor.builder()
            .activityQuestionValue("questionValue")
            .activityAnswerValue("answerValue")
            .activityAnswerLetter("answerLetter")
            .activityQuestionNumber("1")
            .activityAnswerPoints(10)
            .build();

        assertEquals("questionValue", descriptor.getActivityQuestionValue());
        assertEquals("answerValue", descriptor.getActivityAnswerValue());
        assertEquals("1", descriptor.getActivityQuestionNumber());
        assertEquals(10, descriptor.getActivityAnswerPoints());
        assertEquals("answerLetter", descriptor.getActivityAnswerLetter());

        assertEquals("Descriptor(activityQuestionNumber=1, "
            + "activityQuestionValue=questionValue, activityAnswerLetter=answerLetter, activityAnswerValue=answerValue, activityAnswerPoints=10)", descriptor.toString());
    }

    @Test
    public void testConstructor() {
        Descriptor descriptor = new Descriptor(
            "1",
            "questionValue",
            "answerLetter",
           "answerValue",
            10);

        assertEquals("questionValue", descriptor.getActivityQuestionValue());
        assertEquals("answerValue", descriptor.getActivityAnswerValue());
        assertEquals("1", descriptor.getActivityQuestionNumber());
        assertEquals(10, descriptor.getActivityAnswerPoints());
        assertEquals("answerLetter", descriptor.getActivityAnswerLetter());

        assertEquals("Descriptor(activityQuestionNumber=1, "
            + "activityQuestionValue=questionValue, activityAnswerLetter=answerLetter, activityAnswerValue=answerValue, activityAnswerPoints=10)", descriptor.toString());
    }

    @Test
    public void testDescriptorEqualsAndHashcode() {
        Descriptor descriptor1 = Descriptor.builder()
            .activityQuestionValue("questionValue")
            .activityAnswerValue("answerValue")
            .activityAnswerLetter("answerLetter")
            .activityQuestionNumber("1")
            .activityAnswerPoints(10)
            .build();

        Descriptor descriptor2 = Descriptor.builder()
            .activityQuestionValue("questionValue")
            .activityAnswerValue("answerValue")
            .activityAnswerLetter("answerLetter")
            .activityQuestionNumber("1")
            .activityAnswerPoints(10)
            .build();

        Descriptor descriptor3 = Descriptor.builder()
            .activityQuestionValue("questionValue2")
            .activityAnswerValue("answerValue")
            .activityAnswerLetter("answerLetter")
            .activityQuestionNumber("1")
            .activityAnswerPoints(10)
            .build();

        assertEquals(descriptor1, descriptor2);
        assertEquals(descriptor1.hashCode(), descriptor2.hashCode());
        assertNotEquals(descriptor1, descriptor3);
        assertNotEquals(descriptor1.hashCode(), descriptor3.hashCode());

    }

}
