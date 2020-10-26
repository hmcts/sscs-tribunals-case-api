package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.FieldConditionBase;

public class YesNoFieldCondition extends FieldConditionBase<YesNo> {

    public YesNoFieldCondition(String fieldName, Predicate<YesNo> predicate,
        Function<SscsCaseData, YesNo> fieldExtractor) {
        super(fieldName, predicate, fieldExtractor);
    }
    
    public Optional<String> getOptionalErrorMessage(SscsCaseData caseData) {

        YesNo value = fieldExtractor.apply(caseData);
        if (!predicate.test(value)) {
            if (YesNoPredicate.UNSPECIFIED.equals(predicate)) {
                return Optional.of("an unexpected answer for the " + fieldName + " question");
            } else {
                if (value == null) {
                    return Optional.of("a missing answer for the " + fieldName + " question");
                } else {
                    return Optional.of(value + " for the " + fieldName + " question");
                }
            }
        }
        return Optional.empty();
    }
}

