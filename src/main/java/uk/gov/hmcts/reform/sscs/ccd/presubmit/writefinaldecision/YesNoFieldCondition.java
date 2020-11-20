package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

public class YesNoFieldCondition extends FieldConditionBase<YesNo> {

    boolean displayIsSatisfiedMessage;

    public YesNoFieldCondition(String fieldName, Predicate<YesNo> predicate,
        Function<SscsCaseData, YesNo> fieldExtractor, boolean displayIsSatisfiedMessage) {
        super(fieldName, predicate, fieldExtractor);
        this.displayIsSatisfiedMessage = displayIsSatisfiedMessage;
    }

    public YesNoFieldCondition(String fieldName, Predicate<YesNo> predicate,
                               Function<SscsCaseData, YesNo> fieldExtractor) {
        this(fieldName, predicate, fieldExtractor, true);
    }
    
    public Optional<String> getOptionalErrorMessage(SscsCaseData caseData) {

        YesNo value = fieldExtractor.apply(caseData);
        if (!predicate.test(value)) {
            if (YesNoPredicate.UNSPECIFIED.equals(predicate)) {
                return Optional.of("submitted an unexpected answer for the " + fieldName + " question");
            } else {
                if (value == null) {
                    return Optional.of("a missing answer for the " + fieldName + " question");
                } else {
                    return Optional.of("answered " + value + " for the " + fieldName + " question");
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getOptionalIsSatisfiedMessage(SscsCaseData caseData) {
        if (displayIsSatisfiedMessage) {
            if (YesNoPredicate.TRUE.equals(predicate)) {
                return Optional.of("specified that " + fieldName + " applies");
            } else if (YesNoPredicate.FALSE.equals(predicate)) {
                return Optional.of("specified that " + fieldName + " does not apply");
            } else if (YesNoPredicate.NOT_TRUE.equals(predicate)) {
                YesNo value = fieldExtractor.apply(caseData);
                if (value != null) {
                    return Optional.of("specified that " + fieldName + " does not apply");
                } else {
                    return Optional.empty();
                }
            } else if (YesNoPredicate.UNSPECIFIED.equals(predicate)) {
                return Optional.of("not provided an answer to the " + fieldName + " question");
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }
}

