package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import java.util.function.Predicate;

public enum AllowedOrRefusedPredicate implements Predicate<String> {

    ALLOWED(v -> "allowed".equals(v)),
    REFUSED(v -> "refused".equals(v));

    Predicate<String> predicate;

    AllowedOrRefusedPredicate(Predicate<String> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean test(String yesNo) {
        return predicate.test(yesNo);
    }
}
