package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import java.util.Comparator;
import org.apache.commons.collections4.ComparatorUtils;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;

public class DescriptorLexicographicalComparator implements Comparator<Descriptor> {

    @Override
    public int compare(Descriptor o1, Descriptor o2) {
        int questionNumberComparison =
            ComparatorUtils.<Integer>naturalComparator()
                .compare(Integer.parseInt(o1.getActivityQuestionNumber()), Integer.parseInt(o2.getActivityQuestionNumber()));
        if (questionNumberComparison != 0) {
            return questionNumberComparison;
        } else {
            return ComparatorUtils.<String>naturalComparator()
                .compare(o1.getActivityAnswerLetter(), o2.getActivityAnswerLetter());
        }
    }
}
