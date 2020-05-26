package uk.gov.hmcts.reform.sscs.util;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

public class PostcodeUtilTest {

    private String postcode = "CM11 1AB";
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
        assertThat(new PostcodeUtil().hasAppellantPostcode(sscsCaseDetails, postcode), is(true));
    }

    @Test
    public void noSpaceIsEquivalent() {
        assertThat(new PostcodeUtil().hasAppellantPostcode(sscsCaseDetails, "CM111AB"), is(true));
    }

    @Test
    public void multipleSpacesAreEquivalent() {
        assertThat(new PostcodeUtil().hasAppellantPostcode(sscsCaseDetails, "CM11    1AB"), is(true));
    }

    @Test
    public void lowerCaseIsEquivalent() {
        assertThat(new PostcodeUtil().hasAppellantPostcode(sscsCaseDetails, "cm11 1ab"), is(true));
    }

    @Test
    public void differentIsNotEquivalent() {
        assertThat(new PostcodeUtil().hasAppellantPostcode(sscsCaseDetails, "CM12 1AB"), is(false));
    }
}
