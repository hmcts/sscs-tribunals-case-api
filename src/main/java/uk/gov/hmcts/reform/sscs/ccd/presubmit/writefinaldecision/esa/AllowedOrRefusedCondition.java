package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import java.util.Optional;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.FieldConditionBase;

public class AllowedOrRefusedCondition extends FieldConditionBase<String> {

    protected AllowedOrRefusedCondition(AllowedOrRefusedPredicate predicate) {
        super("Allowed or Refused", predicate, c -> c.getWriteFinalDecisionAllowedOrRefused());
    }

    @Override
    public Optional<String> getOptionalErrorMessage(SscsCaseData caseData) {
        String value = fieldExtractor.apply(caseData);
        if (!predicate.test(value)) {
            if (value == null) {
                return Optional.of("a missing answer for the " + fieldName + " question");
            } else {
                return Optional.of(value + " for the " + fieldName + " question");
            }
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> getOptionalIsSatisfiedMessage() {
        boolean displayIsSatisfiedMessage = true;
        if (displayIsSatisfiedMessage) {
            if (AllowedOrRefusedPredicate.ALLOWED.equals(predicate)) {
                return Optional.of("specified that the appeal is allowed");
            } else if (AllowedOrRefusedPredicate.REFUSED.equals(predicate)) {
                return Optional.of("specified that the appeal is refused");
            } else {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }
}
