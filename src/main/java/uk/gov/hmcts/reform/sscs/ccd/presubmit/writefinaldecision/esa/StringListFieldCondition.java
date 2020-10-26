package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.FieldConditionBase;

public class StringListFieldCondition extends FieldConditionBase<List<String>> {

    public StringListFieldCondition(String fieldName, Predicate<List<String>> predicate,
        Function<SscsCaseData, List<String>> fieldExtractor) {
        super(fieldName, predicate, fieldExtractor);
    }
    
    public Optional<String> getOptionalErrorMessage(SscsCaseData caseData) {

        List<String> value = fieldExtractor.apply(caseData);
        if (!predicate.test(value)) {
            if (StringListPredicate.UNSPECIFIED.equals(predicate)) {
                return Optional.of("an unexpected answer for the " + fieldName + " question");
            } else {
                if (value == null) {
                    return Optional.of("a missing answer for the " + fieldName + " question");
                } else {
                    if (value.isEmpty()) {
                        return Optional.of("no selections for the " + fieldName + " question");
                    } else {
                        return Optional.of(value.size() + " selections for the " + fieldName + " question");
                    }
                }
            }
        }
        return Optional.empty();
    }
}

