package uk.gov.hmcts.reform.sscs.util.predicates;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import java.util.Optional;
import java.util.function.BiPredicate;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class FirstOtherPartyAddedPredicate implements BiPredicate<SscsCaseData, SscsCaseData> {

    private static final int FIRST_OTHER_PARTY_COUNT = 1;

    @Override
    public boolean test(final SscsCaseData newSscsCaseData, final SscsCaseData caseDataBefore) {
        return isEmpty(Optional.ofNullable(caseDataBefore).map(SscsCaseData::getOtherParties).orElse(emptyList()))
            && newSscsCaseData.getOtherParties().size() == FIRST_OTHER_PARTY_COUNT;
    }
}
