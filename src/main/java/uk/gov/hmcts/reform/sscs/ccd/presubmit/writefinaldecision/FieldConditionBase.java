package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import java.util.function.Function;
import java.util.function.Predicate;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public abstract class FieldConditionBase<F> implements FieldCondition {

    protected String fieldName;
    protected Predicate<F> predicate;
    protected Function<SscsCaseData, F> fieldExtractor;

    protected FieldConditionBase(String fieldName, Predicate<F> predicate, Function<SscsCaseData, F> fieldExtractor) {
        this.fieldName = fieldName;
        this.predicate = predicate;
        this.fieldExtractor = fieldExtractor;
    }

    public boolean isSatisified(SscsCaseData caseData) {
        return predicate.test(fieldExtractor.apply(caseData));
    }
}
