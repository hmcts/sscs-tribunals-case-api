package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class StringListFieldCondition extends FieldConditionBase<List<String>> {

    public StringListFieldCondition(String fieldName, Predicate<List<String>> predicate,
        Function<SscsCaseData, List<String>> fieldExtractor) {
        super(fieldName, predicate, fieldExtractor);
    }
    
    public Optional<String> getOptionalErrorMessage(SscsCaseData caseData) {

        List<String> value = fieldExtractor.apply(caseData);
        if (!predicate.test(value)) {
            if (StringListPredicate.UNSPECIFIED.equals(predicate)) {
                return Optional.of("submitted an unexpected answer for the " + fieldName + " question");
            } else {
                if (value == null) {
                    return Optional.of("have a missing answer for the " + fieldName + " question");
                } else {
                    if (value.isEmpty()) {
                        return Optional.of("made no selections for the " + fieldName + " question");
                    } else {
                        return Optional.of("made selections for the " + fieldName + " question");
                    }
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getOptionalIsSatisfiedMessage(SscsCaseData caseData) {
        if (StringListPredicate.EMPTY.equals(predicate)) {
            return Optional.of("made no selections for the " + fieldName + " question");
        } else if (StringListPredicate.NOT_EMPTY.equals(predicate)) {
            return Optional.of("made selections for the " + fieldName + " question");
        } else {
            return Optional.empty();
        }
    }
}

