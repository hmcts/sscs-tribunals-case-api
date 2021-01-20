package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import java.util.function.Predicate;

public enum ComparedToDwpPredicate implements Predicate<String> {

    SAME("same"::equals),
    LOWER("lower"::equals),
    HIGHER("higher"::equals),
    NOT_CONSIDERED(s -> s == null);

    Predicate<String> predicate;

    ComparedToDwpPredicate(Predicate<String> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean test(String yesNo) {
        return predicate.test(yesNo);
    }
}
