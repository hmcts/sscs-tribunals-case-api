package uk.gov.hmcts.reform.sscs.util.predicates;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

class FirstOtherPartyAddedPredicateTest {

    private static final CcdValue<OtherParty> OTHER_PARTY_1 = otherParty("1");
    private static final CcdValue<OtherParty> OTHER_PARTY_2 = otherParty("2");

    private final FirstOtherPartyAddedPredicate predicate = new FirstOtherPartyAddedPredicate();

    @Test
    void givenFirstOtherPartyAddedAndNoPreviousOtherParties_thenPredicateIsTrue() {
        final SscsCaseData newData = caseData(List.of(OTHER_PARTY_1));
        final SscsCaseData caseDataBefore = caseData(List.of());

        assertThat(predicate.test(newData, caseDataBefore)).isTrue();
    }

    @Test
    void givenFirstOtherPartyAddedAndNullCaseDataBefore_thenPredicateIsTrue() {
        final SscsCaseData newData = caseData(List.of(OTHER_PARTY_1));

        assertThat(predicate.test(newData, null)).isTrue();
    }

    @Test
    void givenFirstOtherPartyAddedAndPreviousOtherPartiesIsNull_thenPredicateIsTrue() {
        final SscsCaseData newData = caseData(List.of(OTHER_PARTY_1));
        final SscsCaseData caseDataBefore = caseData(null);

        assertThat(predicate.test(newData, caseDataBefore)).isTrue();
    }

    @Test
    void givenMoreThanOneOtherPartyAndNoPreviousOtherParties_thenPredicateIsFalse() {
        final SscsCaseData newData = caseData(List.of(OTHER_PARTY_1, OTHER_PARTY_2));
        final SscsCaseData caseDataBefore = caseData(List.of());

        assertThat(predicate.test(newData, caseDataBefore)).isFalse();
    }

    @Test
    void givenOneOtherPartyAndPreviousOtherPartiesAlreadyExisted_thenPredicateIsFalse() {
        final SscsCaseData newData = caseData(List.of(OTHER_PARTY_1));
        final SscsCaseData caseDataBefore = caseData(List.of(OTHER_PARTY_1));

        assertThat(predicate.test(newData, caseDataBefore)).isFalse();
    }

    private static CcdValue<OtherParty> otherParty(final String id) {
        return CcdValue.<OtherParty>builder().value(OtherParty.builder().id(id).build()).build();
    }

    private static SscsCaseData caseData(final List<CcdValue<OtherParty>> otherParties) {
        return SscsCaseData.builder()
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(Benefit.UC.getShortName()).build()).build())
            .otherParties(otherParties)
            .build();
    }
}
