package uk.gov.hmcts.reform.sscs.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;

public class PostcodeUtilTest {

    private final String postcode = "CM11 1AB";
    private SscsCaseDetails sscsCaseDetails;

    @Before
    public void setUp() {
        sscsCaseDetails = SscsCaseDetails.builder()
                .data(SscsCaseData.builder()
                        .appeal(Appeal.builder()
                                .appellant(Appellant.builder()
                                        .address(Address.builder()
                                                .postcode(postcode)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Test
    public void exactMatchIsEquivalent() {
        assertThat(new PostcodeUtil().hasAppellantOrOtherPartyPostcode(sscsCaseDetails, postcode), is(true));
    }

    @Test
    public void noSpaceIsEquivalent() {
        assertThat(new PostcodeUtil().hasAppellantOrOtherPartyPostcode(sscsCaseDetails, "CM111AB"), is(true));
    }

    @Test
    public void multipleSpacesAreEquivalent() {
        assertThat(new PostcodeUtil().hasAppellantOrOtherPartyPostcode(sscsCaseDetails, "CM11    1AB"), is(true));
    }

    @Test
    public void lowerCaseIsEquivalent() {
        assertThat(new PostcodeUtil().hasAppellantOrOtherPartyPostcode(sscsCaseDetails, "cm11 1ab"), is(true));
    }

    @Test
    public void differentIsNotEquivalent() {
        assertThat(new PostcodeUtil().hasAppellantOrOtherPartyPostcode(sscsCaseDetails, "CM12 1AB"), is(false));
    }

    @Test
    public void willMatchAndOtherPartyPostcode() {
        String otherPartyPostCode = "W1 1LA";
        OtherParty otherParty = OtherParty.builder().address(Address.builder().postcode(otherPartyPostCode).build()).build();
        sscsCaseDetails.getData().setOtherParties(List.of(new CcdValue<>(otherParty)));
        assertThat(new PostcodeUtil().hasAppellantOrOtherPartyPostcode(sscsCaseDetails, "w1 1lA"), is(true));
    }

    @Test
    public void willNotMatchAndOtherPartyOrAppellantsPostcodeIfIncorrectPostcodeIsGiven() {
        String otherPartyPostCode = "W1 1LA";
        OtherParty otherParty = OtherParty.builder().address(Address.builder().postcode(otherPartyPostCode).build()).build();
        sscsCaseDetails.getData().setOtherParties(List.of(new CcdValue<>(otherParty)));
        assertThat(new PostcodeUtil().hasAppellantOrOtherPartyPostcode(sscsCaseDetails, "inc 1ab"), is(false));
    }
}
