package uk.gov.hmcts.reform.sscs.util;

import static java.time.LocalDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.getOtherPartyName;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.getOtherPartyUcb;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.hasNewOtherPartyAdded;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.haveOtherPartiesChanged;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.isConfidential;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.isValidBenefitTypeForConfidentiality;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.sendNewOtherPartyNotification;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.updateOtherPartiesConfidentialityChangedDate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.JointParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNoUndetermined;

public class OtherPartyDataUtilTest {

    public static final String ID_1 = "17a74540-c1b6-49e2-a81b-a9dbd2259251";
    public static final String ID_2 = "2ca270ca-0738-4536-8846-7cea34ff8762";
    public static final String ID_3 = "3a9c1e2a-9536-4aa2-b63d-7cd874e582e3";
    public static final String ID_4 = "440d0d83-75e1-466a-bacc-90ce9e612074";
    private static final int UUID_SIZE = 36;
    private List<CcdValue<OtherParty>> before;
    private List<CcdValue<OtherParty>> after;

    @Test
    void givenUcbIsYesForOneOtherParty_thenSetCaseDataOtherPartyUcb() {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherParty(ID_1, true), buildOtherParty(ID_2, false));

        assertThat(getOtherPartyUcb(otherParties)).isEqualTo(YesNo.YES.getValue());
    }

    @Test
    void givenUcbIsNoForAllOtherParty_thenSetCaseDataOtherPartyUcb() {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherParty(ID_1, false), buildOtherParty(ID_2, false));

        assertThat(getOtherPartyUcb(otherParties)).isEqualTo(YesNo.NO.getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"UPDATE_OTHER_PARTY_DATA", "DWP_UPLOAD_RESPONSE"})
    void givenNewOtherPartyAdded_thenAssignAnIdAndNotificationFlag(EventType eventType) {
        List<CcdValue<OtherParty>> otherParties = singletonList(buildOtherPartyWithAppointeeAndRep(null, null, null));

        otherParties.forEach(otherPartyCcdValue -> otherPartyCcdValue.getValue()
            .setSendNewOtherPartyNotification(sendNewOtherPartyNotification(otherPartyCcdValue)));

        assertThat(otherParties).hasSize(1).extracting(CcdValue::getValue).anySatisfy((OtherParty otherParty) -> {
            assertThat(otherParty.getId()).hasSize(UUID_SIZE);
            assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
            assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
        });
    }

    @Test
    void givenExistingOtherPartiesInUpdateOtherParty_thenNewOtherPartyAssignedNewIdAndSetNotificationFlagForOnlyNewOnes() {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherPartyWithNotificationFlag(ID_2, true),
            buildOtherParty(ID_1), buildOtherPartyWithAppointeeAndRep(null, null, null));

        otherParties.forEach(otherPartyCcdValue -> otherPartyCcdValue.getValue()
            .setSendNewOtherPartyNotification(sendNewOtherPartyNotification(otherPartyCcdValue)));

        assertThat(otherParties).hasSize(3).extracting(CcdValue::getValue).anySatisfy((OtherParty otherParty) -> {
            assertThat(otherParty.getId()).isEqualTo(ID_1);
            assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);

        }).anySatisfy((OtherParty otherParty) -> {
            assertThat(otherParty.getId()).isEqualTo(ID_2);
            assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(NO);
        }).anySatisfy((OtherParty otherParty) -> {
            assertThat(otherParty.getId()).isNotEqualTo(ID_1).isNotEqualTo(ID_2).hasSize(UUID_SIZE);
            assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
            assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
        });
    }

    @Test
    void givenExistingOtherPartiesWithAppointeeAndRep_thenNewOtherPartyAssignedNewId() {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherParty(ID_2),
            buildOtherPartyWithAppointeeAndRep(ID_1, ID_3, ID_4), buildOtherPartyWithAppointeeAndRep(null, null, null));

        otherParties.forEach(otherPartyCcdValue -> otherPartyCcdValue.getValue()
            .setSendNewOtherPartyNotification(sendNewOtherPartyNotification(otherPartyCcdValue)));

        assertThat(otherParties).hasSize(3).extracting(CcdValue::getValue).anySatisfy((OtherParty otherParty) -> {
            assertThat(otherParty.getId()).isEqualTo(ID_1);
            assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            assertThat(otherParty.getAppointee().getId()).isEqualTo(ID_3);
            assertThat(otherParty.getRep().getId()).isEqualTo(ID_4);
        }).anySatisfy((OtherParty otherParty) -> {
            assertThat(otherParty.getId()).isEqualTo(ID_2);
            assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
        }).anySatisfy((OtherParty otherParty) -> {
            assertThat(otherParty.getId()).isNotEqualTo(ID_1).isNotEqualTo(ID_2).hasSize(UUID_SIZE);
            assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
            assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
        });
    }

    @Test
    void givenExistingOtherParties_thenNewOtherPartyAppointeeAndRepAssignedNewId() {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherPartyWithAppointeeAndRep(ID_2, null, null),
            buildOtherPartyWithAppointeeAndRep(ID_1, ID_3, ID_4), buildOtherParty(null));

        otherParties.forEach(otherPartyCcdValue -> otherPartyCcdValue.getValue()
            .setSendNewOtherPartyNotification(sendNewOtherPartyNotification(otherPartyCcdValue)));

        assertThat(otherParties).hasSize(3).extracting(CcdValue::getValue).anySatisfy((OtherParty otherParty) -> {
            assertThat(otherParty.getId()).isEqualTo(ID_1);
            assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            assertThat(otherParty.getAppointee().getId()).isEqualTo(ID_3);
            assertThat(otherParty.getRep().getId()).isEqualTo(ID_4);
        }).anySatisfy((OtherParty otherParty) -> {
            assertThat(otherParty.getId()).isEqualTo(ID_2);
            assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
            assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
        }).anySatisfy((OtherParty otherParty) -> {
            assertThat(otherParty.getId()).isNotEqualTo(ID_1).isNotEqualTo(ID_2).hasSize(UUID_SIZE);
            assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
        });
    }

    @ParameterizedTest
    @MethodSource("buildOtherPartyBeforeAndAfterCollections")
    void givenNewOtherPartyAdded_thenReturnTrue(List<CcdValue<OtherParty>> before, List<CcdValue<OtherParty>> after,
        boolean hasNewOtherParty) {
        assertThat(hasNewOtherPartyAdded(before, after)).isEqualTo(hasNewOtherParty);
    }

    static Object[] buildOtherPartyBeforeAndAfterCollections() {
        return new Object[]{new Object[]{null, null, false}, new Object[]{null, List.of(), false}, new Object[]{null, List.of(
            buildOtherParty(ID_1)), true}, new Object[]{List.of(), List.of(buildOtherParty(ID_1)), true}, new Object[]{List.of(
            buildOtherParty(ID_1), buildOtherParty(ID_2)), List.of(buildOtherParty(ID_1),
            buildOtherParty(ID_2)), false}, new Object[]{List.of(buildOtherParty(ID_1), buildOtherParty(ID_2)), List.of(
            buildOtherParty(ID_1), buildOtherParty(ID_2), buildOtherParty(ID_3)), true}, new Object[]{List.of(
            buildOtherParty(ID_1), buildOtherParty(ID_2)), List.of(buildOtherParty(ID_1), buildOtherParty(ID_3)), true},};
    }

    @Test
    void testComparingListsOfOtherParties() {
        before = singletonList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build());
        after = singletonList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build());

        assertThat(haveOtherPartiesChanged(before, after)).isFalse();
    }

    @Test
    void testComparingListsOfOtherPartiesRemoved() {
        before = singletonList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build());
        after = emptyList();

        assertThat(haveOtherPartiesChanged(before, after)).isTrue();
    }

    @Test
    void testComparingListsOfOtherPartiesDifferentIds() {
        before = singletonList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build());
        after = singletonList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_2").build()).build());

        assertThat(haveOtherPartiesChanged(before, after)).isTrue();
    }

    @Test
    void testComparingListsOfOtherPartiesOrder() {
        before = Arrays.asList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build(),
            CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_2").build()).build());
        after = Arrays.asList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_2").build()).build(),
            CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build());

        assertThat(haveOtherPartiesChanged(before, after)).isFalse();
    }

    @Test
    void testComparingListsOfOtherPartiesNullId() {
        before = singletonList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build());
        after = singletonList(CcdValue.<OtherParty>builder().value(OtherParty.builder().build()).build());

        assertThat(haveOtherPartiesChanged(before, after)).isTrue();
    }

    @Test
    void testComparingListsOfOtherPartiesNullList() {
        before = null;
        after = null;

        assertThat(haveOtherPartiesChanged(before, after)).isFalse();
    }

    @Test
    void testComparingListsOfOtherPartiesNullBfore() {
        before = null;
        after = singletonList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build());

        assertThat(haveOtherPartiesChanged(before, after)).isTrue();
    }

    @Test
    void testComparingListsOfOtherPartiesNullAfter() {
        before = singletonList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build());
        after = null;

        assertThat(haveOtherPartiesChanged(before, after)).isTrue();
    }

    @Test
    void getOtherPartyNameFromId_withNullReturnsNull() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().build();
        String otherPartyName = getOtherPartyName(sscsCaseData, (String) null);
        assertThat(otherPartyName).isNull();
    }

    @ParameterizedTest
    @CsvSource({"1, OtherParty 1", "3, Appointee 3", "4, Rep 4"})
    void getOtherPartyNameFromId_forOtherPartyRepReturnsRepName(String otherPartyId, String expectedName) {
        List<CcdValue<OtherParty>> otherParties = List.of(buildOtherParty("1"),
            buildOtherPartyWithAppointeeAndRep("2", "3", "4"));
        SscsCaseData sscsCaseData = SscsCaseData.builder().otherParties(otherParties).build();
        String otherPartyName = getOtherPartyName(sscsCaseData, otherPartyId);
        assertThat(otherPartyName).isEqualTo(expectedName);
    }

    @Test
    void updateOtherPartiesConfidentialityChangedDate_whenNoPreviousParties_updatesDateForAllCurrentParties() {
        final LocalDateTime originalDate = now().minusHours(1);
        final List<CcdValue<OtherParty>> current = List.of(buildOtherPartyWithConfidentiality(ID_1, YesNoUndetermined.YES, originalDate));

        updateOtherPartiesConfidentialityChangedDate(current, null);

        assertThat(current.getFirst().getValue().getConfidentialityRequiredChangedDate()).isAfter(originalDate);
    }

    @Test
    void updateOtherPartiesConfidentialityChangedDate_whenNoMatchingPreviousPartyById_updatesDate() {
        final LocalDateTime originalDate = now().minusHours(1);
        final List<CcdValue<OtherParty>> previous = List.of(buildOtherPartyWithConfidentiality(ID_2, YesNoUndetermined.YES, originalDate));
        final List<CcdValue<OtherParty>> current = List.of(buildOtherPartyWithConfidentiality(ID_1, YesNoUndetermined.YES, originalDate));

        updateOtherPartiesConfidentialityChangedDate(current, previous);

        assertThat(current.getFirst().getValue().getConfidentialityRequiredChangedDate()).isAfter(originalDate);
    }

    @Test
    void updateConfidentialityChangedDate_whenConfidentialityUnchanged_doesNotUpdateOtherPartiesDate() {
        final LocalDateTime originalDate = now().minusHours(1);
        final List<CcdValue<OtherParty>> previous = List.of(buildOtherPartyWithConfidentiality(ID_1, YesNoUndetermined.YES, originalDate));
        final List<CcdValue<OtherParty>> current = List.of(buildOtherPartyWithConfidentiality(ID_1, YesNoUndetermined.YES, originalDate));

        updateOtherPartiesConfidentialityChangedDate(current, previous);

        assertThat(current.getFirst().getValue().getConfidentialityRequiredChangedDate()).isEqualTo(originalDate);
    }

    @Test
    void updateConfidentialityChangedDate_whenOtherPartiesConfidentialityChanged_updatesDate() {
        final LocalDateTime originalDate = now().minusHours(1);
        final List<CcdValue<OtherParty>> previous = List.of(buildOtherPartyWithConfidentiality(ID_1, YesNoUndetermined.NO, originalDate));
        final List<CcdValue<OtherParty>> current = List.of(buildOtherPartyWithConfidentiality(ID_1, YesNoUndetermined.YES, originalDate));

        updateOtherPartiesConfidentialityChangedDate(current, previous);

        assertThat(current.getFirst().getValue().getConfidentialityRequiredChangedDate()).isAfter(originalDate);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("isConfidentialScenarios")
    void isConfidential_returnsExpectedResult(final String scenario, final SscsCaseData caseData,
        final boolean cmOtherPartyConfidentialityEnabled, final YesNo expected) {
        assertThat(isConfidential(caseData, cmOtherPartyConfidentialityEnabled)).isEqualTo(expected);
    }

    static Stream<Arguments> isConfidentialScenarios() {
        return Stream.of(
            Arguments.of("null case data returns null", null, false, null),
            Arguments.of("null appeal returns null", SscsCaseData.builder().build(), false, null),
            Arguments.of("null benefit type returns null", buildSscsCaseData(null), false, null),
            Arguments.of("universal credit without confidentiality flag returns null", buildSscsCaseData(Benefit.UC), false, null),
            Arguments.of("appellant confidentiality yes returns yes",
                buildSscsCaseData(Benefit.CHILD_SUPPORT, YesNoUndetermined.YES), false, YesNo.YES),
            Arguments.of("other party confidentiality yes with appellant not yes returns yes",
                withOtherParties(buildSscsCaseData(Benefit.CHILD_SUPPORT),
                    buildOtherParty("otherparty-1", true, YesNoUndetermined.YES)), false, YesNo.YES),
            Arguments.of("appellant no and no other parties returns no",
                buildSscsCaseData(Benefit.CHILD_SUPPORT, YesNoUndetermined.NO), false, YesNo.NO),
            Arguments.of("appellant no but other party undetermined returns undetermined",
                withOtherParties(buildSscsCaseData(Benefit.CHILD_SUPPORT, YesNoUndetermined.NO),
                    buildOtherParty("otherparty-1", true, YesNoUndetermined.UNDETERMINED)), false, null),
            Arguments.of("appellant undetermined and no other party confidentiality returns undetermined",
                buildSscsCaseData(Benefit.CHILD_SUPPORT), false, null),
            Arguments.of("universal credit with confidentiality flag enabled and appellant yes returns yes",
                buildSscsCaseData(Benefit.UC, YesNoUndetermined.YES), true, YesNo.YES),
            Arguments.of("universal credit with confidentiality flag enabled and appellant no returns no",
                buildSscsCaseData(Benefit.UC, YesNoUndetermined.NO), true, YesNo.NO),
            Arguments.of("universal credit with confidentiality flag enabled and no confidentiality returns undetermined",
                buildSscsCaseData(Benefit.UC), true, null)
        );
    }

    private static SscsCaseData withOtherParties(final SscsCaseData caseData, final CcdValue<OtherParty> otherParty) {
        caseData.setOtherParties(List.of(otherParty));
        return caseData;
    }

    @ParameterizedTest
    @MethodSource("benefitsWithSsCsType2And5")
    void givenBenefitWithSsCsType2And5_thenReturnTrue(Benefit benefit) {
        assertThat(isValidBenefitTypeForConfidentiality(buildBenefitType(benefit))).isTrue();
    }

    @Test
    void givenBenefitTypesUniversalCredit_thenReturnFalse() {
        assertThat(isValidBenefitTypeForConfidentiality(buildBenefitType(Benefit.UC))).isFalse();
    }

    @Test
    void givenBenefitTypesUniversalCreditAndCmFlagEnabled_thenReturnTrue() {
        assertThat(isValidBenefitTypeForConfidentiality(buildBenefitType(Benefit.UC), true)).isTrue();
    }

    @Test
    void givenBenefitTypesUniversalCreditAndCmFlagDisabled_thenReturnFalse() {
        assertThat(isValidBenefitTypeForConfidentiality(buildBenefitType(Benefit.UC), false)).isFalse();
    }

    static Stream<Benefit> benefitsWithSsCsType2And5() {
        return Stream.of(Benefit.CHILD_SUPPORT, Benefit.TAX_CREDIT, Benefit.GUARDIANS_ALLOWANCE,
            Benefit.TAX_FREE_CHILDCARE, Benefit.HOME_RESPONSIBILITIES_PROTECTION, Benefit.CHILD_BENEFIT,
            Benefit.THIRTY_HOURS_FREE_CHILDCARE, Benefit.GUARANTEED_MINIMUM_PENSION,
            Benefit.NATIONAL_INSURANCE_CREDITS);
    }

    private static CcdValue<OtherParty> buildOtherParty(String id) {
        return buildOtherParty(id, true);
    }

    private static CcdValue<OtherParty> buildOtherParty(String id, boolean ucb, YesNoUndetermined confidentialityRequired) {
        return CcdValue.<OtherParty>builder().value(
            OtherParty.builder().id(id).name(name("OtherParty", id)).unacceptableCustomerBehaviour(ucb ? YesNo.YES : YesNo.NO)
                .confidentialityRequirement(confidentialityRequired)
                .build()).build();
    }

    private static CcdValue<OtherParty> buildOtherParty(String id, boolean ucb) {
        return CcdValue.<OtherParty>builder().value(
            OtherParty.builder().id(id).name(name("OtherParty", id)).unacceptableCustomerBehaviour(ucb ? YesNo.YES : YesNo.NO)
                .build()).build();
    }

    private CcdValue<OtherParty> buildOtherPartyWithNotificationFlag(String id, boolean sendNotification) {
        return CcdValue.<OtherParty>builder()
            .value(OtherParty.builder().id(id).sendNewOtherPartyNotification(sendNotification ? YES : NO).build()).build();
    }

    private CcdValue<OtherParty> buildOtherPartyWithAppointeeAndRep(String id, String appointeeId, String repId) {
        return CcdValue.<OtherParty>builder().value(
            OtherParty.builder().id(id).name(name("OtherParty", id)).isAppointee(YES.getValue())
                .appointee(Appointee.builder().id(appointeeId).name(name("Appointee", appointeeId)).build())
                .rep(Representative.builder().id(repId).hasRepresentative(YES.getValue()).name(name("Rep", repId)).build())
                .build()).build();
    }

    private static Name name(String name, String id) {
        return Name.builder().firstName(name).lastName(id).build();
    }

    private CcdValue<OtherParty> buildOtherPartyWithConfidentiality(final String id, final YesNoUndetermined confidentiality,
        final LocalDateTime changedDate) {
        return CcdValue.<OtherParty>builder().value(
            OtherParty.builder().id(id).confidentialityRequirement(confidentiality).confidentialityRequiredChangedDate(changedDate)
                .build()).build();
    }

    private static SscsCaseData buildSscsCaseData(Benefit benefit) {
        return buildSscsCaseData(benefit, null);
    }

    private static SscsCaseData buildSscsCaseData(Benefit benefit, YesNoUndetermined confidentialityRequired) {
        var benefitType = benefit != null
            ? buildBenefitType(benefit)
            : null;
        return SscsCaseData.builder()
            .ccdCaseId("ccdId")
            .appeal(Appeal.builder()
                .benefitType(benefitType)
                .appellant(Appellant.builder()
                    .name(Name.builder().firstName("First").lastName("Last").build())
                    .address(Address.builder().line1("Line1").line2("Line2").postcode("CM120NS").build())
                    .identity(Identity.builder().nino("AB223344B").dob("1995-12-20").build())
                    .isAppointee("Yes")
                    .confidentialityRequirement(confidentialityRequired)
                    .appointee(Appointee.builder()
                        .address(Address.builder().line1("123 the Street").postcode("CM120NS").build())
                        .build()).build())
                .rep(Representative.builder()
                    .address(Address.builder().line1("123 the Street").postcode("CM120NS").build()).build())
                .build())
            .jointParty(JointParty.builder().jointPartyAddressSameAsAppellant(NO)
                .address(Address.builder().line1("123 the street").postcode("CM120NS").build()).build())
            .benefitCode("002")
            .issueCode("DD")
            .isFqpmRequired(NO)
            .build();
    }

    private static BenefitType buildBenefitType(Benefit benefit) {
        return BenefitType.builder().code(benefit.getShortName()).description(benefit.getDescription()).build();
    }
}
