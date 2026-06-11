package uk.gov.hmcts.reform.sscs.util.predicates;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

class AdditionalOtherPartyAddedPredicateTest {

    private static final CcdValue<OtherParty> EXISTING_PARTY = otherParty("1");
    private static final CcdValue<OtherParty> NEW_PARTY = otherParty("2");

    @Test
    void givenFewerThanTwoOtherParties_thenPredicateIsFalse() {
        final AdditionalOtherPartyAddedPredicate predicate = new AdditionalOtherPartyAddedPredicate();

        final SscsCaseData newData = caseData(Benefit.CHILD_SUPPORT.getShortName(), List.of(EXISTING_PARTY));
        final CaseDetails<SscsCaseData> caseDetailsBefore = caseDetailsBefore(Benefit.CHILD_SUPPORT.getShortName(), List.of(EXISTING_PARTY));

        assertThat(predicate.test(newData, caseDetailsBefore.getCaseData())).isFalse();
    }

    @Test
    void givenOtherPartiesUnchangedById_thenPredicateIsFalse() {
        final AdditionalOtherPartyAddedPredicate predicate = new AdditionalOtherPartyAddedPredicate();

        final SscsCaseData newData = caseData(Benefit.CHILD_SUPPORT.getShortName(), List.of(EXISTING_PARTY, NEW_PARTY));
        final CaseDetails<SscsCaseData> caseDetailsBefore = caseDetailsBefore(Benefit.CHILD_SUPPORT.getShortName(), List.of(EXISTING_PARTY, NEW_PARTY));

        assertThat(predicate.test(newData, caseDetailsBefore.getCaseData())).isFalse();
    }

    @Test
    void givenNewOtherPartyAddedForChildSupport_thenPredicateIsTrue() {
        final AdditionalOtherPartyAddedPredicate predicate = new AdditionalOtherPartyAddedPredicate();

        final SscsCaseData newData = caseData(Benefit.CHILD_SUPPORT.getShortName(), List.of(EXISTING_PARTY, NEW_PARTY));
        final CaseDetails<SscsCaseData> caseDetailsBefore = caseDetailsBefore(Benefit.CHILD_SUPPORT.getShortName(), List.of(EXISTING_PARTY));

        assertThat(predicate.test(newData, caseDetailsBefore.getCaseData())).isTrue();
    }

    @Test
    void givenNewOtherPartyAddedForUc_thenPredicateIsTrue() {
        final AdditionalOtherPartyAddedPredicate predicate = new AdditionalOtherPartyAddedPredicate();

        final SscsCaseData newData = caseData(Benefit.UC.getShortName(), List.of(EXISTING_PARTY, NEW_PARTY));
        final CaseDetails<SscsCaseData> caseDetailsBefore = caseDetailsBefore(Benefit.UC.getShortName(), List.of(EXISTING_PARTY));

        assertThat(predicate.test(newData, caseDetailsBefore.getCaseData())).isTrue();
    }

    @Test
    void givenNullOldDataWithMultipleOtherParties_thenPredicateIsTrue() {
        final AdditionalOtherPartyAddedPredicate predicate = new AdditionalOtherPartyAddedPredicate();

        final SscsCaseData newData = caseData(Benefit.CHILD_SUPPORT.getShortName(), List.of(EXISTING_PARTY, NEW_PARTY));

        assertThat(predicate.test(newData, null)).isTrue();
    }

    private static CcdValue<OtherParty> otherParty(final String id) {
        return CcdValue.<OtherParty>builder().value(OtherParty.builder().id(id).build()).build();
    }

    private static SscsCaseData caseData(final String benefitCode, final List<CcdValue<OtherParty>> otherParties) {
        return SscsCaseData.builder()
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefitCode).build()).build())
            .otherParties(otherParties)
            .build();
    }

    private static CaseDetails<SscsCaseData> caseDetailsBefore(final String benefitCode, final List<CcdValue<OtherParty>> otherParties) {
        return new CaseDetails<>(0L, null, null, caseData(benefitCode, otherParties), null, null);
    }
}
