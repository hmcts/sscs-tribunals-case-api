package uk.gov.hmcts.reform.sscs.util;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

public class SscsUtilTest {

    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice("3").build()).build()).build();
    }

    @Test
    public void givenCaseWithAppellant_thenGetPartiesOnCaseWithAppellant() {
        List<DynamicListItem> response = SscsUtil.getPartiesOnCase(sscsCaseData);

        assertEquals(1, response.size());
        assertEquals("appellant", response.get(0).getCode());
    }

    @Test
    public void givenCaseWithRep_thenGetPartiesOnCaseWithAppellantAndRep() {
        sscsCaseData.getAppeal().setRep(Representative.builder().hasRepresentative("yes").build());

        List<DynamicListItem> response = SscsUtil.getPartiesOnCase(sscsCaseData);

        assertEquals(2, response.size());
        assertEquals("appellant", response.get(0).getCode());
        assertEquals("representative", response.get(1).getCode());
    }

    @Test
    public void givenCaseWithJointParty_thenGetPartiesOnCaseWithAppellantAndJointParty() {
        sscsCaseData.setJointParty("Yes");

        List<DynamicListItem> response = SscsUtil.getPartiesOnCase(sscsCaseData);

        assertEquals(2, response.size());
        assertEquals("appellant", response.get(0).getCode());
        assertEquals("jointParty", response.get(1).getCode());
    }

    @Test
    public void givenRequestToGetListWithDwpAndHmcts_thenGetPartiesOnCaseWithAppellantAndDwpAndHmcts() {
        List<DynamicListItem> response = SscsUtil.getPartiesOnCaseWithDwpAndHmcts(sscsCaseData);

        assertEquals(3, response.size());
        assertEquals("appellant", response.get(0).getCode());
        assertEquals("dwp", response.get(1).getCode());
        assertEquals("hmcts", response.get(2).getCode());
    }

}