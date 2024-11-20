package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;

public class DescriptorComparatorTest {
    
    @Test
    public void testCorrectOrderWhenQuestionNumberSameAndAnswerLetterDifferent() {

        Descriptor descriptor1 = Descriptor.builder().activityQuestionNumber("2").activityAnswerLetter("b").build();
        Descriptor descriptor2 = Descriptor.builder().activityQuestionNumber("2").activityAnswerLetter("a").build();

        List<Descriptor> descriptors = Arrays.asList(descriptor1, descriptor2);

        descriptors.sort(new DescriptorLexicographicalComparator());

        Assertions.assertEquals("a", descriptors.get(0).getActivityAnswerLetter());
        Assertions.assertEquals("b", descriptors.get(1).getActivityAnswerLetter());

    }

    @Test
    public void testCorrectOrderWhenQuestionNumberDifferentAndAnswerLetterDifferent() {

        Descriptor descriptor1 = Descriptor.builder().activityQuestionNumber("3").activityAnswerLetter("b").build();
        Descriptor descriptor2 = Descriptor.builder().activityQuestionNumber("2").activityAnswerLetter("a").build();

        List<Descriptor> descriptors = Arrays.asList(descriptor1, descriptor2);

        descriptors.sort(new DescriptorLexicographicalComparator());

        Assertions.assertEquals("2", descriptors.get(0).getActivityQuestionNumber());
        Assertions.assertEquals("a", descriptors.get(0).getActivityAnswerLetter());

        Assertions.assertEquals("3", descriptors.get(1).getActivityQuestionNumber());
        Assertions.assertEquals("b", descriptors.get(1).getActivityAnswerLetter());

    }

    @Test
    public void testCorrectOrderWhenQuestionNumberDifferentAndAnswerLetterSame() {

        Descriptor descriptor1 = Descriptor.builder().activityQuestionNumber("3").activityAnswerLetter("a").build();
        Descriptor descriptor2 = Descriptor.builder().activityQuestionNumber("2").activityAnswerLetter("a").build();

        List<Descriptor> descriptors = Arrays.asList(descriptor1, descriptor2);

        descriptors.sort(new DescriptorLexicographicalComparator());

        Assertions.assertEquals("2", descriptors.get(0).getActivityQuestionNumber());
        Assertions.assertEquals("a", descriptors.get(0).getActivityAnswerLetter());

        Assertions.assertEquals("3", descriptors.get(1).getActivityQuestionNumber());
        Assertions.assertEquals("a", descriptors.get(1).getActivityAnswerLetter());

    }

}
