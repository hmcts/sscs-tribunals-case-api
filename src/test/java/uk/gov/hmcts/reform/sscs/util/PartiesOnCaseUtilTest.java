package uk.gov.hmcts.reform.sscs.util;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.PartyItemList;

public class PartiesOnCaseUtilTest {

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder()
                .benefitType(BenefitType.builder().code(CHILD_SUPPORT.getShortName()).build())
                .mrnDetails(MrnDetails.builder()
                        .dwpIssuingOffice("3").build()).build()).build();
    }

    @Test
    public void givenCaseWithAppellant_thenGetPartiesOnCaseWithAppellant() {
        List<DynamicListItem> response = PartiesOnCaseUtil.getPartiesOnCase(sscsCaseData);

        assertEquals(1, response.size());
        assertEquals("appellant", response.get(0).getCode());
    }

    @Test
    public void givenCaseWithRep_thenGetPartiesOnCaseWithAppellantAndRep() {
        sscsCaseData.getAppeal().setRep(Representative.builder().hasRepresentative("yes").build());

        List<DynamicListItem> response = PartiesOnCaseUtil.getPartiesOnCase(sscsCaseData);

        assertEquals(2, response.size());
        assertEquals("appellant", response.get(0).getCode());
        assertEquals("representative", response.get(1).getCode());
    }

    @Test
    public void givenCaseWithJointParty_thenGetPartiesOnCaseWithAppellantAndJointParty() {
        sscsCaseData.getJointParty().setHasJointParty(YES);

        List<DynamicListItem> response = PartiesOnCaseUtil.getPartiesOnCase(sscsCaseData);

        assertEquals(2, response.size());
        assertEquals("appellant", response.get(0).getCode());
        assertEquals("jointParty", response.get(1).getCode());
    }

    @Test
    public void givenRequestToGetListWithDwpAndHmcts_thenGetPartiesOnCaseWithAppellantAndDwpAndHmcts() {
        List<DynamicListItem> response = PartiesOnCaseUtil.getPartiesOnCaseWithDwpAndHmcts(sscsCaseData);

        assertEquals(3, response.size());
        assertEquals("appellant", response.get(0).getCode());
        assertEquals("dwp", response.get(1).getCode());
        assertEquals("hmcts", response.get(2).getCode());
    }

    @Test
    public void willGetOtherPartyAndRepOnChildSupportAppeal() {

        OtherParty otherParty = OtherParty.builder()
                .id("1")
                .name(Name.builder().firstName("Bo").lastName("Surname").title("Mr").build())
                .rep(Representative.builder()
                        .id("2")
                        .hasRepresentative(YES.getValue())
                        .name(Name.builder().firstName("Harry").lastName("Rep").build())
                        .build())
                .build();
        sscsCaseData.setOtherParties(List.of(new CcdValue<>(otherParty)));
        List<DynamicListItem> response = PartiesOnCaseUtil.getPartiesOnCase(sscsCaseData);
        assertEquals(3, response.size());
        assertEquals(PartyItemList.APPELLANT.getCode(), response.get(0).getCode());
        assertEquals(PartyItemList.OTHER_PARTY.getCode() + "1", response.get(1).getCode());
        assertEquals("Other party 1 - Bo Surname", response.get(1).getLabel());
        assertEquals(PartyItemList.OTHER_PARTY_REPRESENTATIVE.getCode() + "2", response.get(2).getCode());
        assertEquals("Other party 1 - Representative - Harry Rep", response.get(2).getLabel());
    }

    @Test
    public void willGetOtherPartyAppointeeAndRepOnChildSupportAppeal() {

        OtherParty otherParty = OtherParty.builder()
                .id("1")
                .name(Name.builder().firstName("Bo").lastName("Surname").title("Mr").build())
                .isAppointee(YES.getValue())
                .appointee(Appointee.builder()
                        .id("2")
                        .name(Name.builder().firstName("Silva").lastName("Lining").build())
                        .build())
                .rep(Representative.builder()
                        .id("3")
                        .hasRepresentative(YES.getValue())
                        .name(Name.builder().firstName("Harry").lastName("Rep").build())
                        .build())
                .build();
        sscsCaseData.setOtherParties(List.of(new CcdValue<>(otherParty)));
        List<DynamicListItem> response = PartiesOnCaseUtil.getPartiesOnCase(sscsCaseData);
        assertEquals(3, response.size());
        assertEquals(PartyItemList.APPELLANT.getCode(), response.get(0).getCode());
        assertEquals(PartyItemList.OTHER_PARTY.getCode() + "2", response.get(1).getCode());
        assertEquals("Other party 1 - Bo Surname / Appointee - Silva Lining", response.get(1).getLabel());
        assertEquals(PartyItemList.OTHER_PARTY_REPRESENTATIVE.getCode() + "3", response.get(2).getCode());
        assertEquals("Other party 1 - Representative - Harry Rep", response.get(2).getLabel());
    }

    @Test
    public void willIncrementCounterOnLabelWhenGetMultipleOtherPartiesOnChildSupportAppeal() {

        setupOtherParties();
        List<DynamicListItem> response = PartiesOnCaseUtil.getPartiesOnCase(sscsCaseData);
        assertEquals(5, response.size());
        assertEquals(PartyItemList.APPELLANT.getCode(), response.get(0).getCode());

        assertEquals(PartyItemList.OTHER_PARTY.getCode() + "2", response.get(1).getCode());
        assertEquals("Other party 1 - Bo Surname / Appointee - Silva Lining", response.get(1).getLabel());

        assertEquals(PartyItemList.OTHER_PARTY_REPRESENTATIVE.getCode() + "3", response.get(2).getCode());
        assertEquals("Other party 1 - Representative - Harry Rep", response.get(2).getLabel());

        assertEquals(PartyItemList.OTHER_PARTY.getCode() + "4", response.get(3).getCode());
        assertEquals("Other party 2 - Cat Snack", response.get(3).getLabel());

        assertEquals(PartyItemList.OTHER_PARTY_REPRESENTATIVE.getCode() + "5", response.get(4).getCode());
        assertEquals("Other party 2 - Representative - Peter Rep", response.get(4).getLabel());

    }

    private void setupOtherParties() {
        OtherParty otherParty1 = OtherParty.builder()
                .id("1")
                .name(Name.builder().firstName("Bo").lastName("Surname").title("Mr").build())
                .isAppointee(YES.getValue())
                .appointee(Appointee.builder()
                        .id("2")
                        .name(Name.builder().firstName("Silva").lastName("Lining").build())
                        .build())
                .rep(Representative.builder()
                        .id("3")
                        .hasRepresentative(YES.getValue())
                        .name(Name.builder().firstName("Harry").lastName("Rep").build())
                        .build())
                .build();

        OtherParty otherParty2 = OtherParty.builder()
                .id("4")
                .name(Name.builder().firstName("Cat").lastName("Snack").title("Mrs").build())
                .rep(Representative.builder()
                        .id("5")
                        .hasRepresentative(YES.getValue())
                        .name(Name.builder().firstName("Peter").lastName("Rep").build())
                        .build())
                .build();
        sscsCaseData.setOtherParties(List.of(new CcdValue<>(otherParty1), new CcdValue<>(otherParty2)));
        List<DynamicListItem> response = PartiesOnCaseUtil.getPartiesOnCase(sscsCaseData);
        assertEquals(5, response.size());
        assertEquals(PartyItemList.APPELLANT.getCode(), response.get(0).getCode());

        assertEquals(PartyItemList.OTHER_PARTY.getCode() + "2", response.get(1).getCode());
        assertEquals("Other party 1 - Bo Surname / Appointee - Silva Lining", response.get(1).getLabel());

        assertEquals(PartyItemList.OTHER_PARTY_REPRESENTATIVE.getCode() + "3", response.get(2).getCode());
        assertEquals("Other party 1 - Representative - Harry Rep", response.get(2).getLabel());

        assertEquals(PartyItemList.OTHER_PARTY.getCode() + "4", response.get(3).getCode());
        assertEquals("Other party 2 - Cat Snack", response.get(3).getLabel());

        assertEquals(PartyItemList.OTHER_PARTY_REPRESENTATIVE.getCode() + "5", response.get(4).getCode());
        assertEquals("Other party 2 - Representative - Peter Rep", response.get(4).getLabel());

    }
}
