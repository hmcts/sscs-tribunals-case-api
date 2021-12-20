package uk.gov.hmcts.reform.sscs.util;


import static org.junit.Assert.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;


public class OtherPartyDataUtilTest {

    @Test
    public void givenUcbIsYesForOneOtherParty_thenSetCaseDataOtherPartyUcb() {
        SscsCaseData data = SscsCaseData.builder().otherParties(Arrays.asList(buildOtherParty("1", true), buildOtherParty("2", false))).build();
        OtherPartyDataUtil.updateOtherPartyUcb(data);
        assertEquals(YesNo.YES.getValue(), data.getOtherPartyUcb());
    }

    @Test
    public void givenUcbIsNoForAllOtherParty_thenSetCaseDataOtherPartyUcb() {
        SscsCaseData data = SscsCaseData.builder().otherParties(Arrays.asList(buildOtherParty("1", false), buildOtherParty("2", false))).build();
        OtherPartyDataUtil.updateOtherPartyUcb(data);
        assertEquals(YesNo.NO.getValue(), data.getOtherPartyUcb());
    }

    @Test
    public void givenNewOtherPartyAdded_thenAssignAnId() {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherPartyWithAppointeeAndRep(null, null, null));

        OtherPartyDataUtil.assignOtherPartyId(otherParties);

        assertEquals(1, otherParties.size());
        assertEquals("1", otherParties.get(0).getValue().getId());
        assertEquals("2", otherParties.get(0).getValue().getAppointee().getId());
        assertEquals("3", otherParties.get(0).getValue().getRep().getId());
    }

    @Test
    public void givenExistingOtherParties_thenNewOtherPartyAssignedNextId() {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherParty("2"), buildOtherParty("1"), buildOtherPartyWithAppointeeAndRep(null, null, null));

        OtherPartyDataUtil.assignOtherPartyId(otherParties);

        assertEquals(3, otherParties.size());
        assertEquals("3", otherParties.get(2).getValue().getId());
        assertEquals("4", otherParties.get(2).getValue().getAppointee().getId());
        assertEquals("5", otherParties.get(2).getValue().getRep().getId());
    }

    @Test
    public void givenExistingOtherPartiesWithAppointeeAndRep_thenNewOtherPartyAssignedNextId() {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherParty("2"), buildOtherPartyWithAppointeeAndRep("1", "3", "4"), buildOtherPartyWithAppointeeAndRep(null, null, null));

        OtherPartyDataUtil.assignOtherPartyId(otherParties);

        assertEquals(3, otherParties.size());
        assertEquals("5", otherParties.get(2).getValue().getId());
        assertEquals("6", otherParties.get(2).getValue().getAppointee().getId());
        assertEquals("7", otherParties.get(2).getValue().getRep().getId());
    }

    @Test
    public void givenExistingOtherParties_thenNewOtherPartyAppointeeAndRepAssignedNextId() {
        List<CcdValue<OtherParty>> otherParties = Arrays.asList(buildOtherPartyWithAppointeeAndRep("2", null, null), buildOtherPartyWithAppointeeAndRep("1", "3", "4"), buildOtherParty(null));

        OtherPartyDataUtil.assignOtherPartyId(otherParties);

        assertEquals(3, otherParties.size());
        assertEquals("5", otherParties.get(0).getValue().getAppointee().getId());
        assertEquals("6", otherParties.get(0).getValue().getRep().getId());
        assertEquals("7", otherParties.get(2).getValue().getId());
    }

    private CcdValue<OtherParty> buildOtherParty(String id) {
        return buildOtherParty(id, true);
    }

    private CcdValue<OtherParty> buildOtherParty(String id, boolean ucb) {
        return CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id(id)
                        .unacceptableCustomerBehaviour(ucb ? YesNo.YES : YesNo.NO)
                        .build())
                .build();
    }

    private CcdValue<OtherParty> buildOtherPartyWithAppointeeAndRep(String id, String appointeeId, String repId) {
        return CcdValue.<OtherParty>builder()
                .value(OtherParty.builder()
                        .id(id)
                        .isAppointee(YES.getValue())
                        .appointee(Appointee.builder().id(appointeeId).build())
                        .rep(Representative.builder().id(repId).hasRepresentative(YES.getValue()).build())
                        .build())
                .build();
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


}
