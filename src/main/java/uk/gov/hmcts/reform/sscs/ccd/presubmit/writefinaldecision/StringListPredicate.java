package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public enum StringListPredicate implements Predicate<List<String>> {

    UNSPECIFIED(Objects::isNull),
    EMPTY(v -> v != null && v.isEmpty()),
    NOT_EMPTY(v -> v != null && !v.isEmpty());

    Predicate<List<String>> predicate;

    StringListPredicate(Predicate<List<String>> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean test(List<String> listOfStrings) {
        return predicate.test(listOfStrings);
    }
}
