package uk.gov.hmcts.reform.sscs.util;


import static java.time.LocalDateTime.now;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.getOtherPartyUcb;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.sendNewOtherPartyNotification;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.updateAppellantConfidentialityRequiredChangedDate;
import static uk.gov.hmcts.reform.sscs.util.OtherPartyDataUtil.updateOtherPartiesConfidentialityChangedDate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.Callback;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;

@RunWith(JUnitParamsRunner.class)
public class OtherPartyDataUtilTest {

    public static final String ID_1 = "17a74540-c1b6-49e2-a81b-a9dbd2259251";
    public static final String ID_2 = "2ca270ca-0738-4536-8846-7cea34ff8762";
    public static final String ID_3 = "3a9c1e2a-9536-4aa2-b63d-7cd874e582e3";
    public static final String ID_4 = "440d0d83-75e1-466a-bacc-90ce9e612074";
    private static final int UUID_SIZE = 36;
    List<CcdValue<OtherParty>> before;
    List<CcdValue<OtherParty>> after;

    @Test
    public void givenUcbIsYesForOneOtherParty_thenSetCaseDataOtherPartyUcb() {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherParty(ID_1, true), buildOtherParty(ID_2, false));

        assertEquals(YesNo.YES.getValue(), getOtherPartyUcb(otherParties));
    }

    @Test
    public void givenUcbIsNoForAllOtherParty_thenSetCaseDataOtherPartyUcb() {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherParty(ID_1, false), buildOtherParty(ID_2, false));

        assertEquals(YesNo.NO.getValue(), getOtherPartyUcb(otherParties));
    }

    @Test
    @Parameters({"UPDATE_OTHER_PARTY_DATA", "DWP_UPLOAD_RESPONSE"})
    public void givenNewOtherPartyAdded_thenAssignAnIdAndNotificationFlag(EventType eventType) {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherPartyWithAppointeeAndRep(null, null, null));

        otherParties.forEach(otherPartyCcdValue -> otherPartyCcdValue.getValue()
            .setSendNewOtherPartyNotification(sendNewOtherPartyNotification(otherPartyCcdValue)));

        Assertions.assertThat(otherParties).hasSize(1).extracting(CcdValue::getValue).anySatisfy((OtherParty otherParty) -> {
            Assertions.assertThat(otherParty.getId()).hasSize(UUID_SIZE);
            Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            Assertions.assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
            Assertions.assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
        });
    }

    @Test
    public void givenExistingOtherPartiesInUpdateOtherParty_thenNewOtherPartyAssignedNewIdAndSetNotificationFlagForOnlyNewOnes() {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherPartyWithNotificationFlag(ID_2, true),
            buildOtherParty(ID_1), buildOtherPartyWithAppointeeAndRep(null, null, null));

        otherParties.forEach(otherPartyCcdValue -> otherPartyCcdValue.getValue()
            .setSendNewOtherPartyNotification(sendNewOtherPartyNotification(otherPartyCcdValue)));

        Assertions.assertThat(otherParties).hasSize(3).extracting(CcdValue::getValue).anySatisfy((OtherParty otherParty) -> {
            Assertions.assertThat(otherParty.getId()).isEqualTo(ID_1);
            Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            ;
        }).anySatisfy((OtherParty otherParty) -> {
            Assertions.assertThat(otherParty.getId()).isEqualTo(ID_2);
            Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(NO);
        }).anySatisfy((OtherParty otherParty) -> {
            Assertions.assertThat(otherParty.getId()).isNotEqualTo(ID_1).isNotEqualTo(ID_2).hasSize(UUID_SIZE);
            Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            Assertions.assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
            Assertions.assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
        });
    }

    @Test
    public void givenExistingOtherPartiesWithAppointeeAndRep_thenNewOtherPartyAssignedNewId() {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherParty(ID_2),
            buildOtherPartyWithAppointeeAndRep(ID_1, ID_3, ID_4), buildOtherPartyWithAppointeeAndRep(null, null, null));

        otherParties.forEach(otherPartyCcdValue -> otherPartyCcdValue.getValue()
            .setSendNewOtherPartyNotification(sendNewOtherPartyNotification(otherPartyCcdValue)));

        Assertions.assertThat(otherParties).hasSize(3).extracting(CcdValue::getValue).anySatisfy((OtherParty otherParty) -> {
            Assertions.assertThat(otherParty.getId()).isEqualTo(ID_1);
            Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            ;
            Assertions.assertThat(otherParty.getAppointee().getId()).isEqualTo(ID_3);
            Assertions.assertThat(otherParty.getRep().getId()).isEqualTo(ID_4);
        }).anySatisfy((OtherParty otherParty) -> {
            Assertions.assertThat(otherParty.getId()).isEqualTo(ID_2);
            Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
        }).anySatisfy((OtherParty otherParty) -> {
            Assertions.assertThat(otherParty.getId()).isNotEqualTo(ID_1).isNotEqualTo(ID_2).hasSize(UUID_SIZE);
            Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            Assertions.assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
            Assertions.assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
        });
    }

    @Test
    public void givenExistingOtherParties_thenNewOtherPartyAppointeeAndRepAssignedNewId() {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherPartyWithAppointeeAndRep(ID_2, null, null),
            buildOtherPartyWithAppointeeAndRep(ID_1, ID_3, ID_4), buildOtherParty(null));

        otherParties.forEach(otherPartyCcdValue -> otherPartyCcdValue.getValue()
            .setSendNewOtherPartyNotification(sendNewOtherPartyNotification(otherPartyCcdValue)));

        Assertions.assertThat(otherParties).hasSize(3).extracting(CcdValue::getValue).anySatisfy((OtherParty otherParty) -> {
            Assertions.assertThat(otherParty.getId()).isEqualTo(ID_1);
            Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            ;
            Assertions.assertThat(otherParty.getAppointee().getId()).isEqualTo(ID_3);
            Assertions.assertThat(otherParty.getRep().getId()).isEqualTo(ID_4);
        }).anySatisfy((OtherParty otherParty) -> {
            Assertions.assertThat(otherParty.getId()).isEqualTo(ID_2);
            Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
            Assertions.assertThat(otherParty.getAppointee().getId()).hasSize(UUID_SIZE);
            Assertions.assertThat(otherParty.getRep().getId()).hasSize(UUID_SIZE);
        }).anySatisfy((OtherParty otherParty) -> {
            Assertions.assertThat(otherParty.getId()).isNotEqualTo(ID_1).isNotEqualTo(ID_2).hasSize(UUID_SIZE);
            Assertions.assertThat(otherParty.getSendNewOtherPartyNotification()).isEqualTo(YES);
        });
    }

    @Test
    @Parameters(method = "buildOtherPartyBeforeAndAfterCollections")
    public void givenNewOtherPartyAdded_thenReturnTrue(List<CcdValue<OtherParty>> before, List<CcdValue<OtherParty>> after,
        boolean hasNewOtherParty) {
        assertEquals(hasNewOtherParty, OtherPartyDataUtil.hasNewOtherPartyAdded(before, after));
    }

    public Object[] buildOtherPartyBeforeAndAfterCollections() {
        return new Object[]{new Object[]{null, null, false}, new Object[]{null, List.of(), false}, new Object[]{null, List.of(
            buildOtherParty(ID_1)), true}, new Object[]{List.of(), List.of(buildOtherParty(ID_1)), true}, new Object[]{List.of(
            buildOtherParty(ID_1), buildOtherParty(ID_2)), List.of(buildOtherParty(ID_1),
            buildOtherParty(ID_2)), false}, new Object[]{List.of(buildOtherParty(ID_1), buildOtherParty(ID_2)), List.of(
            buildOtherParty(ID_1), buildOtherParty(ID_2), buildOtherParty(ID_3)), true}, new Object[]{List.of(
            buildOtherParty(ID_1), buildOtherParty(ID_2)), List.of(buildOtherParty(ID_1), buildOtherParty(ID_3)), true},};
    }

    @Test
    public void testComparingListsOfOtherParties() {
        before = singletonList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build());
        after = singletonList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build());

        assertFalse(OtherPartyDataUtil.haveOtherPartiesChanged(before, after));
    }

    @Test
    public void testComparingListsOfOtherPartiesRemoved() {
        before = singletonList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build());
        after = emptyList();

        assertTrue(OtherPartyDataUtil.haveOtherPartiesChanged(before, after));
    }

    @Test
    public void testComparingListsOfOtherPartiesDifferentIds() {
        before = singletonList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build());
        after = singletonList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_2").build()).build());

        assertTrue(OtherPartyDataUtil.haveOtherPartiesChanged(before, after));
    }

    @Test
    public void testComparingListsOfOtherPartiesOrder() {
        before = Arrays.asList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build(),
            CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_2").build()).build());
        after = Arrays.asList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_2").build()).build(),
            CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build());

        assertFalse(OtherPartyDataUtil.haveOtherPartiesChanged(before, after));
    }

    @Test
    public void testComparingListsOfOtherPartiesNullId() {
        before = Arrays.asList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build());
        after = Arrays.asList(CcdValue.<OtherParty>builder().value(OtherParty.builder().build()).build());

        assertTrue(OtherPartyDataUtil.haveOtherPartiesChanged(before, after));
    }

    @Test
    public void testComparingListsOfOtherPartiesNullList() {
        before = null;
        after = null;

        assertFalse(OtherPartyDataUtil.haveOtherPartiesChanged(before, after));
    }

    @Test
    public void testComparingListsOfOtherPartiesNullBfore() {
        before = null;
        after = singletonList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build());
        ;

        assertTrue(OtherPartyDataUtil.haveOtherPartiesChanged(before, after));
    }

    @Test
    public void testComparingListsOfOtherPartiesNullAfter() {
        before = singletonList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build());
        ;
        after = null;

        assertTrue(OtherPartyDataUtil.haveOtherPartiesChanged(before, after));
    }

    @Test
    public void getOtherPartyNameFromId_withNullReturnsNull() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().build();
        String otherPartyName = OtherPartyDataUtil.getOtherPartyName(sscsCaseData, (String) null);
        assertThat(otherPartyName).isNull();
    }

    @Test
    @Parameters({"1, OtherParty 1", "3, Appointee 3", "4, Rep 4"})

    public void getOtherPartyNameFromId_forOtherPartyRepReturnsRepName(String otherPartyId, String expectedName) {
        List<CcdValue<OtherParty>> otherParties = List.of(buildOtherParty("1"),
            buildOtherPartyWithAppointeeAndRep("2", "3", "4"));
        SscsCaseData sscsCaseData = SscsCaseData.builder().otherParties(otherParties).build();
        String otherPartyName = OtherPartyDataUtil.getOtherPartyName(sscsCaseData, otherPartyId);
        assertThat(otherPartyName).isEqualTo(expectedName);
    }

    @Test
    public void updateOtherPartiesConfidentialityChangedDate_whenNoPreviousParties_updatesDateForAllCurrentParties() {
        final LocalDateTime originalDate = now().minusHours(1);
        final List<CcdValue<OtherParty>> current = List.of(buildOtherPartyWithConfidentiality(ID_1, YES, originalDate));

        updateOtherPartiesConfidentialityChangedDate(current, null);

        assertThat(current.getFirst().getValue().getConfidentialityRequiredChangedDate()).isAfter(originalDate);
    }

    @Test
    public void updateOtherPartiesConfidentialityChangedDate_whenNoMatchingPreviousPartyById_updatesDate() {
        final LocalDateTime originalDate = now().minusHours(1);
        final List<CcdValue<OtherParty>> previous = List.of(buildOtherPartyWithConfidentiality(ID_2, YES, originalDate));
        final List<CcdValue<OtherParty>> current = List.of(buildOtherPartyWithConfidentiality(ID_1, YES, originalDate));

        updateOtherPartiesConfidentialityChangedDate(current, previous);

        assertThat(current.getFirst().getValue().getConfidentialityRequiredChangedDate()).isAfter(originalDate);
    }

    @Test
    public void updateConfidentialityChangedDate_whenConfidentialityUnchanged_doesNotUpdateOtherPartiesDate() {
        final LocalDateTime originalDate = now().minusHours(1);
        final List<CcdValue<OtherParty>> previous = List.of(buildOtherPartyWithConfidentiality(ID_1, YES, originalDate));
        final List<CcdValue<OtherParty>> current = List.of(buildOtherPartyWithConfidentiality(ID_1, YES, originalDate));

        updateOtherPartiesConfidentialityChangedDate(current, previous);

        assertThat(current.getFirst().getValue().getConfidentialityRequiredChangedDate()).isEqualTo(originalDate);
    }

    @Test
    public void updateConfidentialityChangedDate_whenOtherPartiesConfidentialityChanged_updatesDate() {
        final LocalDateTime originalDate = now().minusHours(1);
        final List<CcdValue<OtherParty>> previous = List.of(buildOtherPartyWithConfidentiality(ID_1, NO, originalDate));
        final List<CcdValue<OtherParty>> current = List.of(buildOtherPartyWithConfidentiality(ID_1, YES, originalDate));

        updateOtherPartiesConfidentialityChangedDate(current, previous);

        assertThat(current.getFirst().getValue().getConfidentialityRequiredChangedDate()).isAfter(originalDate);
    }

    @Test
    public void updateAppellantConfidentialityRequiredChangedDate_whenNoPreviousCaseData_updatesAppellantDate() {
        final LocalDateTime originalDate = now().minusHours(1);
        final Appellant appellant = Appellant.builder().confidentialityRequired(YES)
            .confidentialityRequiredChangedDate(originalDate).build();
        final SscsCaseData currentCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).appellant(appellant).build())
            .build();

        final Callback<SscsCaseData> callback = buildCallback(currentCaseData, null);

        updateAppellantConfidentialityRequiredChangedDate(callback);

        assertThat(appellant.getConfidentialityRequiredChangedDate()).isAfter(originalDate);
    }

    @Test
    public void updateConfidentialityRequiredChangedDate_whenAppellantConfidentialityChanges_updatesAppellantDate() {
        final LocalDateTime originalDate = now().minusHours(1);
        final Appellant appellantBefore = Appellant.builder().confidentialityRequired(NO).build();
        final SscsCaseData beforeCaseData = SscsCaseData.builder().appeal(Appeal.builder().appellant(appellantBefore).build())
            .build();
        final Appellant appellant = Appellant.builder().confidentialityRequired(YES)
            .confidentialityRequiredChangedDate(originalDate).build();
        final SscsCaseData currentCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).appellant(appellant).build())
            .build();

        final Callback<SscsCaseData> callback = buildCallback(currentCaseData, beforeCaseData);

        updateAppellantConfidentialityRequiredChangedDate(callback);

        assertThat(appellant.getConfidentialityRequiredChangedDate()).isAfter(originalDate);
    }

    @Test
    public void updateConfidentialityRequiredChangedDate_whenConfidentialityUnchanged_doesNotUpdateAppellantAppellantDate() {
        final LocalDateTime originalDate = now().minusHours(1);
        final Appellant appellantBefore = Appellant.builder().confidentialityRequired(YES).build();
        final SscsCaseData beforeCaseData = SscsCaseData.builder().appeal(Appeal.builder().appellant(appellantBefore).build())
            .build();
        final Appellant appellant = Appellant.builder().confidentialityRequired(YES)
            .confidentialityRequiredChangedDate(originalDate).build();
        final SscsCaseData currentCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).appellant(appellant).build())
            .build();

        final Callback<SscsCaseData> callback = buildCallback(currentCaseData, beforeCaseData);

        updateAppellantConfidentialityRequiredChangedDate(callback);

        assertThat(appellant.getConfidentialityRequiredChangedDate()).isEqualTo(originalDate);
    }

    @Test
    public void updateOtherPartiesConfidentialityChangedDate_whenCurrentConfidentialityIsNull_doesNotUpdateDate() {
        final List<CcdValue<OtherParty>> previous = List.of(buildOtherPartyWithConfidentiality(ID_1, null, null));
        final List<CcdValue<OtherParty>> current = List.of(buildOtherPartyWithConfidentiality(ID_1, null, null));

        updateOtherPartiesConfidentialityChangedDate(current, previous);

        assertThat(current.getFirst().getValue().getConfidentialityRequiredChangedDate()).isNull();
    }

    @Test
    public void updateAppellantConfidentialityRequiredChangedDate_whenCurrentConfidentialityIsNull_doesNotUpdateDate() {
        final Appellant appellantBefore = Appellant.builder().build();
        final SscsCaseData beforeCaseData = SscsCaseData.builder().appeal(Appeal.builder().appellant(appellantBefore).build())
            .build();
        final Appellant appellant = Appellant.builder().confidentialityRequired(null).confidentialityRequiredChangedDate(null)
            .build();
        final SscsCaseData currentCaseData = SscsCaseData.builder()
            .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).appellant(appellant).build())
            .build();

        final Callback<SscsCaseData> callback = buildCallback(currentCaseData, beforeCaseData);

        updateAppellantConfidentialityRequiredChangedDate(callback);

        assertThat(appellant.getConfidentialityRequiredChangedDate()).isNull();
    }

    private CcdValue<OtherParty> buildOtherParty(String id) {
        return buildOtherParty(id, true);
    }

    private CcdValue<OtherParty> buildOtherParty(String id, boolean ucb) {
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

    private Name name(String name, String id) {
        return Name.builder().firstName(name).lastName(id).build();
    }

    private CcdValue<OtherParty> buildOtherPartyWithConfidentiality(final String id, final YesNo confidentiality,
        final LocalDateTime changedDate) {
        return CcdValue.<OtherParty>builder().value(
            OtherParty.builder().id(id).confidentialityRequired(confidentiality).confidentialityRequiredChangedDate(changedDate)
                .build()).build();
    }

    @SuppressWarnings("unchecked")
    private Callback<SscsCaseData> buildCallback(final SscsCaseData currentCaseData, final SscsCaseData previousCaseData) {
        final Callback<SscsCaseData> callback = mock(Callback.class);
        final CaseDetails<SscsCaseData> caseDetails = mock(CaseDetails.class);
        when(callback.getCaseDetails()).thenReturn(caseDetails);
        when(caseDetails.getCaseData()).thenReturn(currentCaseData);

        if (nonNull(previousCaseData)) {
            final CaseDetails<SscsCaseData> caseDetailsBefore = mock(CaseDetails.class);
            when(caseDetailsBefore.getCaseData()).thenReturn(previousCaseData);
            when(callback.getCaseDetailsBefore()).thenReturn(Optional.of(caseDetailsBefore));
        } else {
            when(callback.getCaseDetailsBefore()).thenReturn(Optional.empty());
        }
        return callback;
    }

}
