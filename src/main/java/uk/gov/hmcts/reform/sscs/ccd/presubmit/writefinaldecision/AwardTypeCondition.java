package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import java.util.Optional;
import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipActivityType;

public class AwardTypeCondition extends FieldConditionBase<String> {

    private PipActivityType activityType;
    private AwardTypePredicate awardTypePredicate;

    public AwardTypeCondition(AwardTypePredicate awardTypePredicate,  Function<SscsCaseData, String> fieldExtractor, PipActivityType activityType) {
        super("What are you considering awarding for " + activityType.getName().toLowerCase(), awardTypePredicate, fieldExtractor);
        this.activityType = activityType;
        this.awardTypePredicate = awardTypePredicate;
    }

    public AwardTypePredicate getAwardTypePredicate() {
        return awardTypePredicate;
    }

    @Override
    public Optional<String> getOptionalErrorMessage(SscsCaseData caseData) {
        String value = fieldExtractor.apply(caseData);
        if (!predicate.test(value)) {
            if (value == null) {
                return Optional.of("a missing answer for the '" + fieldName + "' question");
            } else {
                return Optional.of(value + " for the '" + fieldName + "' question");
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getOptionalIsSatisfiedMessage(SscsCaseData sscsCaseData) {
        if (AwardTypePredicate.CONSIDERED.equals(predicate)) {
            return Optional.of("specified that the award for " + activityType.getName().toLowerCase() + " has been considered ");
        } else {
            return Optional.empty();
        }
    }
}
