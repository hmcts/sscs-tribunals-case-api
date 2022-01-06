package uk.gov.hmcts.reform.sscs.util;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import org.junit.Before;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

public class PostcodeUtilTest {

    private static final String EMAIL = "email@example.com";
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
        assertThat(new PostcodeUtil().hasAppellantOrOtherPartyPostcode(sscsCaseDetails, postcode, EMAIL), is(true));
    }

    @Test
    public void noSpaceIsEquivalent() {
        assertThat(new PostcodeUtil().hasAppellantOrOtherPartyPostcode(sscsCaseDetails, "CM111AB", EMAIL), is(true));
    }

    @Test
    public void multipleSpacesAreEquivalent() {
        assertThat(new PostcodeUtil().hasAppellantOrOtherPartyPostcode(sscsCaseDetails, "CM11    1AB", EMAIL), is(true));
    }

    @Test
    public void lowerCaseIsEquivalent() {
        assertThat(new PostcodeUtil().hasAppellantOrOtherPartyPostcode(sscsCaseDetails, "cm11 1ab", EMAIL), is(true));
    }

    @Test
    public void differentIsNotEquivalent() {
        assertThat(new PostcodeUtil().hasAppellantOrOtherPartyPostcode(sscsCaseDetails, "CM12 1AB", EMAIL), is(false));
    }

    @Test
    public void willMatchAndOtherPartyPostcode() {
        String otherPartyPostCode = "W1 1LA";
        OtherParty otherParty = OtherParty.builder()
                .otherPartySubscription(Subscription.builder().email(EMAIL).build())
                .address(Address.builder().postcode(otherPartyPostCode).build()).build();
        sscsCaseDetails.getData().setOtherParties(List.of(new CcdValue<>(otherParty)));
        assertThat(new PostcodeUtil().hasAppellantOrOtherPartyPostcode(sscsCaseDetails, "w1 1lA", EMAIL), is(true));
    }

    @Test
    public void willMatchAndOtherPartyPostcodeOnOtherPartyAppointeeEmail() {
        String otherPartyPostCode = "W1 1LA";
        OtherParty otherParty = OtherParty.builder()
                .isAppointee("Yes")
                .appointee(Appointee.builder().address(Address.builder().postcode("w2 2la").build()).build())
                .otherPartyAppointeeSubscription(Subscription.builder().email(EMAIL).build())
                .otherPartySubscription(Subscription.builder().email("otherParty@example.com").build())
                .address(Address.builder().postcode(otherPartyPostCode).build()).build();
        sscsCaseDetails.getData().setOtherParties(List.of(new CcdValue<>(otherParty)));
        assertThat(new PostcodeUtil().hasAppellantOrOtherPartyPostcode(sscsCaseDetails, "w1 1lA", EMAIL), is(true));
    }

    @Test
    public void willMatchAndOtherPartyPostcodeOnOtherPartyRepEmail() {
        String otherPartyPostCode = "W1 1LA";
        OtherParty otherParty = OtherParty.builder()
                .rep(Representative.builder().hasRepresentative("Yes")
                        .address(Address.builder().postcode("w2 2la").build()).build())
                .otherPartyRepresentativeSubscription(Subscription.builder().email(EMAIL).build())
                .otherPartySubscription(Subscription.builder().email("otherParty@example.com").build())
                .address(Address.builder().postcode(otherPartyPostCode).build()).build();
        sscsCaseDetails.getData().setOtherParties(List.of(new CcdValue<>(otherParty)));
        assertThat(new PostcodeUtil().hasAppellantOrOtherPartyPostcode(sscsCaseDetails, "w1 1lA", EMAIL), is(true));
    }

    @Test
    public void willNotMatchAndOtherPartyPostcodeIfEmailDoesNotMatch() {
        String otherPartyPostCode = "W1 1LA";
        OtherParty otherParty1 = OtherParty.builder()
                .otherPartySubscription(Subscription.builder().email("otherParty@example.com").build())
                .address(Address.builder().postcode(otherPartyPostCode).build()).build();
        OtherParty otherParty2 = OtherParty.builder()
                .otherPartySubscription(Subscription.builder().email(EMAIL).build())
                .address(Address.builder().postcode("W2 2LA").build()).build();
        sscsCaseDetails.getData().setOtherParties(List.of(new CcdValue<>(otherParty1), new CcdValue<>(otherParty2)));
        assertThat(new PostcodeUtil().hasAppellantOrOtherPartyPostcode(sscsCaseDetails, "w1 1lA", EMAIL), is(false));
    }

    @Test
    public void willNotMatchAndOtherPartyOrAppellantsPostcodeIfIncorrectPostcodeIsGiven() {
        String otherPartyPostCode = "W1 1LA";
        OtherParty otherParty = OtherParty.builder().address(Address.builder().postcode(otherPartyPostCode).build()).build();
        sscsCaseDetails.getData().setOtherParties(List.of(new CcdValue<>(otherParty)));
        assertThat(new PostcodeUtil().hasAppellantOrOtherPartyPostcode(sscsCaseDetails, "inc 1ab", EMAIL), is(false));
    }
}
