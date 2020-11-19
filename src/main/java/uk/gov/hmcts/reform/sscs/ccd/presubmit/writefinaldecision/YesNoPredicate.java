package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import java.util.function.Predicate;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

public enum YesNoPredicate implements Predicate<YesNo> {

    UNSPECIFIED(v -> v == null),
    SPECIFIED(v -> v != null),
    TRUE(v -> v != null && v.toBoolean()),
    FALSE(v -> v != null && !v.toBoolean()),
    NOT_TRUE(v -> v == null || !v.toBoolean()),
    ANY(v -> true);

    Predicate<YesNo> predicate;

    YesNoPredicate(Predicate<YesNo> predicate) {
        this.predicate = predicate;
    }

    @Override
    public boolean test(YesNo yesNo) {
        return predicate.test(yesNo);
    }
}
