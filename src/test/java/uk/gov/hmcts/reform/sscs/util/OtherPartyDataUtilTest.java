package uk.gov.hmcts.reform.sscs.util;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.assertj.core.groups.Tuple;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class OtherPartyDataUtilTest {

    public static final String ID_1 = "17a74540-c1b6-49e2-a81b-a9dbd2259251";
    public static final String ID_2 = "2ca270ca-0738-4536-8846-7cea34ff8762";
    public static final String ID_3 = "3a9c1e2a-9536-4aa2-b63d-7cd874e582e3";
    public static final String ID_4 = "440d0d83-75e1-466a-bacc-90ce9e612074";
    public static final String ID_5 = "569df4d6-2e1a-43af-a188-8c725d40f8d5";

    @Test
    public void givenUcbIsYesForOneOtherParty_thenSetCaseDataOtherPartyUcb() {
        SscsCaseData data = SscsCaseData.builder().otherParties(Arrays.asList(
            buildOtherParty(ID_1, true),
            buildOtherParty(ID_2, false))).build();
        OtherPartyDataUtil.updateOtherPartyUcb(data);
        assertEquals(YesNo.YES.getValue(), data.getOtherPartyUcb());
    }

    @Test
    public void givenUcbIsNoForAllOtherParty_thenSetCaseDataOtherPartyUcb() {
        SscsCaseData data = SscsCaseData.builder().otherParties(Arrays.asList(
            buildOtherParty(ID_1, false),
            buildOtherParty(ID_2, false))).build();
        OtherPartyDataUtil.updateOtherPartyUcb(data);
        assertEquals(YesNo.NO.getValue(), data.getOtherPartyUcb());
    }

    @Test
    @Parameters({"UPDATE_OTHER_PARTY_DATA", "DWP_UPLOAD_RESPONSE"})
    public void givenNewOtherPartyAdded_thenAssignAnIdAndNotificationFlag(EventType eventType) {


        List<CcdValue<OtherParty>> otherParties = Arrays.asList(
            buildOtherPartyWithAppointeeAndRep(null, null, null));

        OtherPartyDataUtil.assignNewOtherPartyData(otherParties);

        assertThat(otherParties)
            .hasSize(1)
            .extracting(CcdValue::getValue)
            .extracting(OtherParty::getSendNewOtherPartyNotification)
            .containsOnly(YES);

        assertThat(otherParties)
            .extracting(CcdValue::getValue)
            .flatExtracting(otherParty -> List.of(otherParty.getAppointee(), otherParty.getRep()))
            .extracting(Entity::getId)
            .hasSize(2)
            .doesNotContainNull()
            .extracting(String::length)
            .containsOnly(36);
    }

    @Test
    public void givenExistingOtherPartiesInUpdateOtherParty_thenNewOtherPartyAssignedNextIdAndSetNotificationFlagForOnlyNewOnes() {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(
            buildOtherPartyWithNotificationFlag(ID_2, true),
            buildOtherParty(ID_1),
            buildOtherPartyWithAppointeeAndRep(null, null, null));

        OtherPartyDataUtil.assignNewOtherPartyData(otherParties);

        assertThat(otherParties)
            .hasSize(3)
            .extracting(CcdValue::getValue)
            .extracting(OtherParty::getId,OtherParty::getSendNewOtherPartyNotification)
            .contains(Tuple.tuple(ID_1, YES), Tuple.tuple(ID_2, NO));

        assertThat(otherParties)
            .extracting(CcdValue::getValue)
            .extracting(Entity::getId)
            .hasSize(3)
            .contains(ID_2, ID_1)
            .doesNotContainNull()
            .extracting(String::length)
            .containsOnly(36);

        assertThat(otherParties)
            .extracting(CcdValue::getValue)
            .filteredOn(x -> !ID_1.equals(x.getId()) && !ID_2.equals(x.getId()))
            .flatExtracting(otherParty -> List.of(otherParty.getAppointee(), otherParty.getRep()))
            .extracting(Entity::getId)
            .hasSize(2)
            .doesNotContainNull()
            .extracting(String::length)
            .containsOnly(36);
    }

    @Test
    public void givenExistingOtherPartiesInDwpResponse_thenNewOtherPartyAssignedNextIdAndSetNotificationFlagForAllPartiesNotSentTheNotificationYet() {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(
            buildOtherPartyWithNotificationFlag(ID_2, true),
            buildOtherParty(ID_1),
            buildOtherPartyWithAppointeeAndRep(null, null, null));

        OtherPartyDataUtil.assignNewOtherPartyData(otherParties);

        assertThat(otherParties)
            .hasSize(3)
            .extracting(CcdValue::getValue)
            .extracting(OtherParty::getId,OtherParty::getSendNewOtherPartyNotification)
            .contains(Tuple.tuple(ID_1, YES), Tuple.tuple(ID_2, NO));

        assertThat(otherParties)
            .extracting(CcdValue::getValue)
            .extracting(Entity::getId)
            .hasSize(3)
            .contains(ID_2, ID_1)
            .doesNotContainNull()
            .extracting(String::length)
            .containsOnly(36);

        assertThat(otherParties)
            .extracting(CcdValue::getValue)
            .filteredOn(x -> !ID_1.equals(x.getId()) && !ID_2.equals(x.getId()))
            .flatExtracting(otherParty -> List.of(otherParty.getAppointee(), otherParty.getRep()))
            .extracting(Entity::getId)
            .hasSize(2)
            .doesNotContainNull()
            .extracting(String::length)
            .containsOnly(36);
    }

    @Test
    public void givenExistingOtherPartiesWithAppointeeAndRep_thenNewOtherPartyAssignedNextId() {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(
            buildOtherParty(ID_2),
            buildOtherPartyWithAppointeeAndRep(ID_1, ID_3, ID_4),
            buildOtherPartyWithAppointeeAndRep(null, null, null));

        OtherPartyDataUtil.assignNewOtherPartyData(otherParties);

        assertThat(otherParties)
            .hasSize(3)
            .extracting(CcdValue::getValue)
            .extracting(OtherParty::getSendNewOtherPartyNotification)
            .containsOnly(YES);

        assertThat(otherParties)
            .extracting(CcdValue::getValue)
            .extracting(Entity::getId)
            .hasSize(3)
            .contains(ID_2, ID_1)
            .doesNotContainNull()
            .extracting(String::length)
            .containsOnly(36);

        assertThat(otherParties)
            .extracting(CcdValue::getValue)
            .filteredOn(otherParty -> ID_1.equals(otherParty.getId()))
            .extracting(
                otherParty -> otherParty.getAppointee().getId(),
                otherParty -> otherParty.getRep().getId())
            .containsOnly(Tuple.tuple(ID_3, ID_4));

        assertThat(otherParties)
            .extracting(CcdValue::getValue)
            .filteredOn(x -> !ID_1.equals(x.getId()) && !ID_2.equals(x.getId()))
            .flatExtracting(otherParty -> List.of(otherParty.getAppointee(), otherParty.getRep()))
            .extracting(Entity::getId)
            .hasSize(2)
            .doesNotContainNull()
            .extracting(String::length)
            .containsOnly(36);
    }

    @Test
    public void givenExistingOtherParties_thenNewOtherPartyAppointeeAndRepAssignedNextId() {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(
            buildOtherPartyWithAppointeeAndRep(ID_2, null, null),
            buildOtherPartyWithAppointeeAndRep(ID_1, ID_3, ID_4),
            buildOtherParty(null));

        OtherPartyDataUtil.assignNewOtherPartyData(otherParties);

        assertThat(otherParties)
            .hasSize(3)
            .extracting(CcdValue::getValue)
            .extracting(OtherParty::getSendNewOtherPartyNotification)
            .containsOnly(YES);

        assertThat(otherParties)
            .extracting(CcdValue::getValue)
            .extracting(Entity::getId)
            .hasSize(3)
            .contains(ID_2, ID_1)
            .doesNotContainNull()
            .extracting(String::length)
            .containsOnly(36);

        assertThat(otherParties)
            .extracting(CcdValue::getValue)
            .filteredOn(otherParty -> ID_1.equals(otherParty.getId()))
            .extracting(
                otherParty -> otherParty.getAppointee().getId(),
                otherParty -> otherParty.getRep().getId())
            .containsOnly(Tuple.tuple(ID_3, ID_4));

        assertThat(otherParties)
            .extracting(CcdValue::getValue)
            .filteredOn(otherParty -> ID_2.equals(otherParty.getId()))
            .flatExtracting(otherParty -> List.of(otherParty.getAppointee(), otherParty.getRep()))
            .extracting(Entity::getId)
            .hasSize(2)
            .doesNotContainNull()
            .extracting(String::length)
            .containsOnly(36);
    }

    @Test
    @Parameters(method = "buildOtherPartyBeforeAndAfterCollections")
    public void givenNewOtherPartyAdded_thenReturnTrue(List<CcdValue<OtherParty>> before, List<CcdValue<OtherParty>> after, boolean hasNewOtherParty) {
        assertEquals(hasNewOtherParty, OtherPartyDataUtil.hasNewOtherPartyAdded(before,after));
    }

    public Object[] buildOtherPartyBeforeAndAfterCollections() {
        return new Object[] {
            new Object[] { null, null, false },
            new Object[] { null, List.of(), false },
            new Object[] { null, List.of(buildOtherParty(ID_1)), true },
            new Object[] { List.of(), List.of(buildOtherParty(ID_1)), true },
            new Object[] { List.of(buildOtherParty(ID_1), buildOtherParty(ID_2)), List.of(buildOtherParty(ID_1), buildOtherParty(ID_2)), false },
            new Object[] { List.of(buildOtherParty(ID_1), buildOtherParty(ID_2)), List.of(buildOtherParty(ID_1), buildOtherParty(ID_2), buildOtherParty(ID_3)), true },
            new Object[] { List.of(buildOtherParty(ID_1), buildOtherParty(ID_2)), List.of(buildOtherParty(ID_1), buildOtherParty(ID_3)), true },
        };
    }

    private CcdValue<OtherParty> buildOtherParty(String id) {
        return buildOtherParty(id, true);
    }

    private CcdValue<OtherParty> buildOtherParty(String id, boolean ucb) {
        return CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id(id)
                        .name(name("OtherParty", id))
                        .unacceptableCustomerBehaviour(ucb ? YesNo.YES : YesNo.NO)
                        .build())
                .build();
    }

    private CcdValue<OtherParty> buildOtherPartyWithNotificationFlag(String id, boolean sendNotification) {
        return CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id(id)
                        .sendNewOtherPartyNotification(sendNotification ? YES : NO)
                        .build())
                .build();
    }

    private CcdValue<OtherParty> buildOtherPartyWithAppointeeAndRep(String id, String appointeeId, String repId) {
        return CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id(id)
                        .name(name("OtherParty", id))
                        .isAppointee(YES.getValue())
                        .appointee(Appointee.builder().id(appointeeId).name(name("Appointee", appointeeId)).build())
                        .rep(Representative.builder().id(repId).hasRepresentative(YES.getValue()).name(name("Rep", repId)).build())
                        .build())
                .build();
    }

    private Name name(String name, String id) {
        return Name.builder().firstName(name).lastName(id).build();
    }

    List<CcdValue<OtherParty>> before;
    List<CcdValue<OtherParty>> after;

    @Test
    public void testComparingListsOfOtherParties() {
        before = Arrays.asList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build());
        after = Arrays.asList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build());

        assertFalse(OtherPartyDataUtil.haveOtherPartiesChanged(before, after));
    }

    @Test
    public void testComparingListsOfOtherPartiesRemoved() {
        before = Arrays.asList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build());
        after = Collections.emptyList();

        assertTrue(OtherPartyDataUtil.haveOtherPartiesChanged(before, after));
    }

    @Test
    public void testComparingListsOfOtherPartiesDifferentIds() {
        before = Arrays.asList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build());
        after = Arrays.asList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_2").build()).build());

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
        after = Arrays.asList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build());;

        assertTrue(OtherPartyDataUtil.haveOtherPartiesChanged(before, after));
    }

    @Test
    public void testComparingListsOfOtherPartiesNullAfter() {
        before = Arrays.asList(CcdValue.<OtherParty>builder().value(OtherParty.builder().id("other_party_1").build()).build());;
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
    @Parameters({
            "1, OtherParty 1",
            "3, Appointee 3",
            "4, Rep 4"
    })

    public void getOtherPartyNameFromId_forOtherPartyRepReturnsRepName(String otherPartyId, String expectedName) {
        List<CcdValue<OtherParty>> otherParties = List.of(
            buildOtherParty("1"),
            buildOtherPartyWithAppointeeAndRep("2", "3", "4"));
        SscsCaseData sscsCaseData = SscsCaseData.builder().otherParties(otherParties).build();
        String otherPartyName = OtherPartyDataUtil.getOtherPartyName(sscsCaseData, otherPartyId);
        assertThat(otherPartyName).isEqualTo(expectedName);
    }

}
