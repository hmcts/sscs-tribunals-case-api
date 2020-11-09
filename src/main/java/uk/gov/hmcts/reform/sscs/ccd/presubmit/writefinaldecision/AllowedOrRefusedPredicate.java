package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import java.util.function.Predicate;

public enum AllowedOrRefusedPredicate implements Predicate<String> {

    ALLOWED("allowed"::equals),
    REFUSED("refused"::equals);

    Predicate<String> predicate;

    AllowedOrRefusedPredicate(Predicate<String> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean test(String yesNo) {
        return predicate.test(yesNo);
    }
}
