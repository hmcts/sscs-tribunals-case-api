package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;

public class DescriptorComparatorTest {
    
    @Test
    public void testCorrectOrderWhenQuestionNumberSameAndAnswerLetterDifferent() {

        Descriptor descriptor1 = Descriptor.builder().activityQuestionNumber("2").activityAnswerLetter("b").build();
        Descriptor descriptor2 = Descriptor.builder().activityQuestionNumber("2").activityAnswerLetter("a").build();

        List<Descriptor> descriptors = Arrays.asList(descriptor1, descriptor2);

        descriptors.sort(new DescriptorLexicographicalComparator());

        Assert.assertEquals("a", descriptors.get(0).getActivityAnswerLetter());
        Assert.assertEquals("b", descriptors.get(1).getActivityAnswerLetter());

    }

    @Test
    public void testCorrectOrderWhenQuestionNumberDifferentAndAnswerLetterDifferent() {

        Descriptor descriptor1 = Descriptor.builder().activityQuestionNumber("3").activityAnswerLetter("b").build();
        Descriptor descriptor2 = Descriptor.builder().activityQuestionNumber("2").activityAnswerLetter("a").build();

        List<Descriptor> descriptors = Arrays.asList(descriptor1, descriptor2);

        descriptors.sort(new DescriptorLexicographicalComparator());

        Assert.assertEquals("2", descriptors.get(0).getActivityQuestionNumber());
        Assert.assertEquals("a", descriptors.get(0).getActivityAnswerLetter());

        Assert.assertEquals("3", descriptors.get(1).getActivityQuestionNumber());
        Assert.assertEquals("b", descriptors.get(1).getActivityAnswerLetter());

    }

    @Test
    public void testCorrectOrderWhenQuestionNumberDifferentAndAnswerLetterSame() {

        Descriptor descriptor1 = Descriptor.builder().activityQuestionNumber("3").activityAnswerLetter("a").build();
        Descriptor descriptor2 = Descriptor.builder().activityQuestionNumber("2").activityAnswerLetter("a").build();

        List<Descriptor> descriptors = Arrays.asList(descriptor1, descriptor2);

        descriptors.sort(new DescriptorLexicographicalComparator());

        Assert.assertEquals("2", descriptors.get(0).getActivityQuestionNumber());
        Assert.assertEquals("a", descriptors.get(0).getActivityAnswerLetter());

        Assert.assertEquals("3", descriptors.get(1).getActivityQuestionNumber());
        Assert.assertEquals("a", descriptors.get(1).getActivityAnswerLetter());

    }

}
