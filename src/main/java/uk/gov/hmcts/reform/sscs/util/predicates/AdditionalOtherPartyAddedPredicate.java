package uk.gov.hmcts.reform.sscs.util.predicates;

import static java.util.Collections.emptyList;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Entity;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class AdditionalOtherPartyAddedPredicate implements BiPredicate<SscsCaseData, SscsCaseData> {

    private static final int MINIMUM_NUMBER_OTHER_PARTIES = 2;

    @Override
    public boolean test(final SscsCaseData newSscsCaseData, final SscsCaseData caseDetailsBefore) {
        if (emptyIfNull(newSscsCaseData.getOtherParties()).size() < MINIMUM_NUMBER_OTHER_PARTIES) {
            return false;
        }
        final Set<String> newParties = getUniqueOtherPartyIds(newSscsCaseData.getOtherParties());
        final Set<String> previousParties = getUniqueOtherPartyIds(
            Optional.ofNullable(caseDetailsBefore).map(SscsCaseData::getOtherParties).orElse(emptyList()));
        return !newParties.equals(previousParties);
    }

    private static Set<String> getUniqueOtherPartyIds(final List<CcdValue<OtherParty>> otherParties) {
        return emptyIfNull(otherParties).stream()
            .map(CcdValue::getValue)
            .map(Entity::getId)
            .collect(Collectors.toSet());
    }
}
