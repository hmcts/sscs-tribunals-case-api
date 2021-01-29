package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import java.util.function.Predicate;

public enum AwardTypePredicate implements Predicate<String> {

    NOT_CONSIDERED(c -> c == null || c.equals("notConsidered")),
    CONSIDERED(NOT_CONSIDERED.negate());

    Predicate<String> predicate;

    AwardTypePredicate(Predicate<String> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean test(String yesNo) {
        return predicate.test(yesNo);
    }
}
