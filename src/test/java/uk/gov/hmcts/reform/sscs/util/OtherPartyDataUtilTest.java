package uk.gov.hmcts.reform.sscs.util;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@RunWith(JUnitParamsRunner.class)
public class OtherPartyDataUtilTest {

    @Test
    public void givenUcbIsYesForOneOtherParty_thenSetCaseDataOtherPartyUcb() {
        SscsCaseData data = SscsCaseData.builder().otherParties(Arrays.asList(buildOtherParty("1", true), buildOtherParty("2", false))).build();
        OtherPartyDataUtil.updateOtherPartyUcb(data);
        assertEquals(YES, data.getOtherPartyUcb());
    }

    @Test
    public void givenUcbIsNoForAllOtherParty_thenSetCaseDataOtherPartyUcb() {
        SscsCaseData data = SscsCaseData.builder().otherParties(Arrays.asList(buildOtherParty("1", false), buildOtherParty("2", false))).build();
        OtherPartyDataUtil.updateOtherPartyUcb(data);
        assertEquals(NO, data.getOtherPartyUcb());
    }

    @Test
    @Parameters({"UPDATE_OTHER_PARTY_DATA", "DWP_UPLOAD_RESPONSE"})
    public void givenNewOtherPartyAdded_thenAssignAnIdAndNotificationFlag(EventType eventType) {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherPartyWithAppointeeAndRep(null, null, null));

        OtherPartyDataUtil.assignNewOtherPartyData(otherParties, eventType);

        assertEquals(1, otherParties.size());
        assertEquals("1", otherParties.get(0).getValue().getId());
        assertEquals("2", otherParties.get(0).getValue().getAppointee().getId());
        assertEquals("3", otherParties.get(0).getValue().getRep().getId());
        assertTrue(isYes(otherParties.get(0).getValue().getSendNewOtherPartyNotification()));
    }

    @Test
    public void givenExistingOtherPartiesInUpdateOtherParty_thenNewOtherPartyAssignedNextIdAndSetNotificationFlagForOnlyNewOnes() {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherPartyWithNotificationFlag("2", true), buildOtherParty("1"), buildOtherPartyWithAppointeeAndRep(null, null, null));

        OtherPartyDataUtil.assignNewOtherPartyData(otherParties, EventType.UPDATE_OTHER_PARTY_DATA);

        assertEquals(3, otherParties.size());
        assertEquals("3", otherParties.get(2).getValue().getId());
        assertEquals("4", otherParties.get(2).getValue().getAppointee().getId());
        assertEquals("5", otherParties.get(2).getValue().getRep().getId());
        assertTrue(isYes(otherParties.get(2).getValue().getSendNewOtherPartyNotification()));
        assertFalse(isYes(otherParties.get(1).getValue().getSendNewOtherPartyNotification()));
        assertFalse(isYes(otherParties.get(0).getValue().getSendNewOtherPartyNotification()));
    }

    @Test
    public void givenExistingOtherPartiesInDwpResponse_thenNewOtherPartyAssignedNextIdAndSetNotificationFlagForAllPartiesNotSentTheNotificationYet() {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherPartyWithNotificationFlag("2", true), buildOtherParty("1"), buildOtherPartyWithAppointeeAndRep(null, null, null));

        OtherPartyDataUtil.assignNewOtherPartyData(otherParties, EventType.DWP_UPLOAD_RESPONSE);

        assertEquals(3, otherParties.size());
        assertEquals("3", otherParties.get(2).getValue().getId());
        assertEquals("4", otherParties.get(2).getValue().getAppointee().getId());
        assertEquals("5", otherParties.get(2).getValue().getRep().getId());
        assertTrue(isYes(otherParties.get(2).getValue().getSendNewOtherPartyNotification()));
        assertTrue(isYes(otherParties.get(1).getValue().getSendNewOtherPartyNotification()));
        assertFalse(isYes(otherParties.get(0).getValue().getSendNewOtherPartyNotification()));
    }

    @Test
    @Parameters({"UPDATE_OTHER_PARTY_DATA", "DWP_UPLOAD_RESPONSE"})
    public void givenExistingOtherPartiesWithAppointeeAndRep_thenNewOtherPartyAssignedNextId(EventType eventType) {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherParty("2"), buildOtherPartyWithAppointeeAndRep("1", "3", "4"), buildOtherPartyWithAppointeeAndRep(null, null, null));

        OtherPartyDataUtil.assignNewOtherPartyData(otherParties, eventType);

        assertEquals(3, otherParties.size());
        assertEquals("5", otherParties.get(2).getValue().getId());
        assertTrue(isYes(otherParties.get(2).getValue().getSendNewOtherPartyNotification()));
        assertEquals("6", otherParties.get(2).getValue().getAppointee().getId());
        assertEquals("7", otherParties.get(2).getValue().getRep().getId());
    }

    @Test
    @Parameters({"UPDATE_OTHER_PARTY_DATA", "DWP_UPLOAD_RESPONSE"})
    public void givenExistingOtherParties_thenNewOtherPartyAppointeeAndRepAssignedNextId(EventType eventType) {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherPartyWithAppointeeAndRep("2", null, null), buildOtherPartyWithAppointeeAndRep("1", "3", "4"), buildOtherParty(null));

        OtherPartyDataUtil.assignNewOtherPartyData(otherParties, eventType);

        assertEquals(3, otherParties.size());
        assertEquals("5", otherParties.get(0).getValue().getAppointee().getId());
        assertEquals("6", otherParties.get(0).getValue().getRep().getId());
        assertEquals("7", otherParties.get(2).getValue().getId());
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
            new Object[] { null, List.of(buildOtherParty("1")), true },
            new Object[] { List.of(), List.of(buildOtherParty("1")), true },
            new Object[] { List.of(buildOtherParty("1"), buildOtherParty("2")), List.of(buildOtherParty("1"), buildOtherParty("2")), false },
            new Object[] { List.of(buildOtherParty("1"), buildOtherParty("2")), List.of(buildOtherParty("1"), buildOtherParty("2"), buildOtherParty("3")), true },
            new Object[] { List.of(buildOtherParty("1"), buildOtherParty("2")), List.of(buildOtherParty("1"), buildOtherParty("3")), true },
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
                        .unacceptableCustomerBehaviour(ucb ? YES : NO)
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
                        .isAppointee(YES)
                        .appointee(Appointee.builder().id(appointeeId).name(name("Appointee", appointeeId)).build())
                        .rep(Representative.builder().id(repId).hasRepresentative(YES).name(name("Rep", repId)).build())
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
        assertThat(otherPartyName, is(nullValue()));
    }

    @Test
    @Parameters({
            "1, OtherParty 1",
            "3, Appointee 3",
            "4, Rep 4"
    })
    public void getOtherPartyNameFromId_forOtherPartyRepReturnsRepName(String otherPartyId, String expectedName) {
        List<CcdValue<OtherParty>> otherParties = List.of(buildOtherParty("1"), buildOtherPartyWithAppointeeAndRep("2", "3", "4"));
        SscsCaseData sscsCaseData = SscsCaseData.builder().otherParties(otherParties).build();
        String otherPartyName = OtherPartyDataUtil.getOtherPartyName(sscsCaseData, otherPartyId);
        assertThat(otherPartyName, is(expectedName));
    }

}
