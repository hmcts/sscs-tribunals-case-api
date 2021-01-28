package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import java.util.Optional;
import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsPipCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.FieldConditionBase;

public class ComparedToDwpCondition extends FieldConditionBase<String> {

    private PipActivityType activityType;

    public ComparedToDwpCondition(ComparedToDwpPredicate predicate, Function<SscsPipCaseData, String> fieldExtractor, PipActivityType activityType) {
        super("compared to DWP", predicate, c -> fieldExtractor.apply(c.getSscsPipCaseData()));
        this.activityType = activityType;
    }

    @Override
    public Optional<String> getOptionalErrorMessage(SscsCaseData caseData) {
        String value = fieldExtractor.apply(caseData);
        if (!predicate.test(value)) {
            if (value == null) {
                return Optional.of("a missing answer for the '" + activityType.getName().toLowerCase() + " " + fieldName + "' question");
            } else {
                return Optional.of(value + " for the '" + activityType.getName().toLowerCase() + " " + fieldName + "' question");
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getOptionalIsSatisfiedMessage(SscsCaseData sscsCaseData) {
        if (ComparedToDwpPredicate.SAME.equals(predicate)) {
            return Optional.of("specified that the award for " + activityType.getName().toLowerCase() + " is the same as that awarded by DWP");
        } else if (ComparedToDwpPredicate.LOWER.equals(predicate)) {
            return Optional.of("specified that the award for " + activityType.getName().toLowerCase() + " is lower than that awarded by DWP");
        } else if (ComparedToDwpPredicate.HIGHER.equals(predicate)) {
            return Optional.of("specified that the award for " + activityType.getName().toLowerCase() + " is higher than that awarded by DWP");
        } else {
            return Optional.empty();
        }
    }
}
