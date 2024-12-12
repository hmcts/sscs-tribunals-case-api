package uk.gov.hmcts.reform.sscs.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;

public class CaseAssignmentVerifierTest {

    private static final String EMAIL = "email@example.com";
    private static final String POSTCODE = "CM11 1AB";
    private static final String IBCA_REFERENCE = "AA11BB22";
    private SscsCaseDetails sscsCaseDetails;

    private CaseAssignmentVerifier underTest;

    @BeforeEach
    public void setUp() {
        sscsCaseDetails = SscsCaseDetails.builder().data(SscsCaseData.builder()
                        .appeal(Appeal.builder()
                                .appellant(Appellant.builder()
                                        .address(Address.builder().postcode(POSTCODE).build())
                                        .identity(Identity.builder().ibcaReference(IBCA_REFERENCE).build())
                                        .build())
                                .build())
                        .build())
                .build();

        underTest = new CaseAssignmentVerifier();
    }

    @Test
    public void postcodeIsExactMatch() {
        assertTrue(underTest.verifyPostcodeOrIbcaReference(sscsCaseDetails, POSTCODE, IBCA_REFERENCE, EMAIL));
    }

    @ParameterizedTest
    @MethodSource("nullAndEmptyValues")
    public void ibcaReferenceIsExactMatch(String postcode) {
        assertTrue(underTest.verifyPostcodeOrIbcaReference(sscsCaseDetails, postcode, IBCA_REFERENCE, EMAIL));
    }

    @ParameterizedTest
    @CsvSource({ "CM111AB", "CM11    1AB", "cm11 1ab" })
    public void shouldMatchPostcodeIrrespectiveOfWhitespaceAndCasing(String postcode) {
        assertTrue(underTest.verifyPostcodeOrIbcaReference(sscsCaseDetails, postcode, IBCA_REFERENCE, EMAIL));
    }

    @ParameterizedTest
    @CsvSource({ " AA11BB22 ", "aa11bb22" })
    public void shouldMatchIbcaReferenceIrrespectiveOfWhitespaceAndCasing(String ibcaReference) {
        assertTrue(underTest.verifyPostcodeOrIbcaReference(sscsCaseDetails, "", ibcaReference, EMAIL));
    }

    @Test
    public void shouldNotMatchPostcode() {
        assertFalse(underTest.verifyPostcodeOrIbcaReference(sscsCaseDetails, "CM12 1AB", IBCA_REFERENCE, EMAIL));
    }

    @Test
    public void shouldNotMatchIbcaReference() {
        assertFalse(underTest.verifyPostcodeOrIbcaReference(sscsCaseDetails, "", "random string", EMAIL));
    }

    @ParameterizedTest
    @MethodSource("nullAndEmptyValues")
    public void shouldNotMatchIfIbcaReferenceIsEmpty(String ibaReference) {
        assertFalse(underTest.verifyPostcodeOrIbcaReference(sscsCaseDetails, "", ibaReference, EMAIL));
    }

    @Test
    public void willMatchOtherPartyPostcode() {
        String otherPartyPostCode = "W1 1LA";
        OtherParty otherParty = OtherParty.builder()
                .otherPartySubscription(Subscription.builder().email(EMAIL).build())
                .address(Address.builder().postcode(otherPartyPostCode).build()).build();
        sscsCaseDetails.getData().setOtherParties(List.of(new CcdValue<>(otherParty)));
        assertTrue(underTest.verifyPostcodeOrIbcaReference(sscsCaseDetails, "w1 1lA", IBCA_REFERENCE, EMAIL));
    }

    @Test
    public void shouldMatchOtherPartyAppointeePostcode() {
        String otherPartyPostCode = "W1 1LA";
        OtherParty otherParty = OtherParty.builder()
                .isAppointee("Yes")
                .appointee(Appointee.builder().address(Address.builder().postcode("w2 2la").build()).build())
                .otherPartyAppointeeSubscription(Subscription.builder().email(EMAIL).build())
                .otherPartySubscription(Subscription.builder().email("otherParty@example.com").build())
                .address(Address.builder().postcode(otherPartyPostCode).build()).build();
        sscsCaseDetails.getData().setOtherParties(List.of(new CcdValue<>(otherParty)));
        assertTrue(underTest.verifyPostcodeOrIbcaReference(sscsCaseDetails, "w1 1lA", IBCA_REFERENCE, EMAIL));
    }

    @Test
    public void shouldMatchOtherPartyRepPostcode() {
        String otherPartyPostCode = "W1 1LA";
        OtherParty otherParty = OtherParty.builder()
                .rep(Representative.builder().hasRepresentative("Yes")
                        .address(Address.builder().postcode("w2 2la").build()).build())
                .otherPartyRepresentativeSubscription(Subscription.builder().email(EMAIL).build())
                .otherPartySubscription(Subscription.builder().email("otherParty@example.com").build())
                .address(Address.builder().postcode(otherPartyPostCode).build()).build();
        sscsCaseDetails.getData().setOtherParties(List.of(new CcdValue<>(otherParty)));
        assertTrue(underTest.verifyPostcodeOrIbcaReference(sscsCaseDetails, "w1 1lA", IBCA_REFERENCE, EMAIL));
    }

    @Test
    public void willNotMatchOtherPartyPostcodeIfEmailDoesNotMatch() {
        String otherPartyPostCode = "W1 1LA";
        OtherParty otherParty1 = OtherParty.builder()
                .otherPartySubscription(Subscription.builder().email("otherParty@example.com").build())
                .address(Address.builder().postcode(otherPartyPostCode).build()).build();
        OtherParty otherParty2 = OtherParty.builder()
                .otherPartySubscription(Subscription.builder().email(EMAIL).build())
                .address(Address.builder().postcode("W2 2LA").build()).build();
        sscsCaseDetails.getData().setOtherParties(List.of(new CcdValue<>(otherParty1), new CcdValue<>(otherParty2)));
        assertFalse(underTest.verifyPostcodeOrIbcaReference(sscsCaseDetails, "w1 1lA", IBCA_REFERENCE, EMAIL));
    }

    @Test
    public void shouldNotMatchOtherPartyPostcodeIfNotInTheList() {
        String otherPartyPostCode = "W1 1LA";
        OtherParty otherParty = OtherParty.builder()
                .address(Address.builder().postcode(otherPartyPostCode).build()).build();
        sscsCaseDetails.getData().setOtherParties(List.of(new CcdValue<>(otherParty)));
        assertFalse(underTest.verifyPostcodeOrIbcaReference(sscsCaseDetails, "inc 1ab", IBCA_REFERENCE, EMAIL));
    }

    static Stream<String> nullAndEmptyValues() {
        return Stream.of(null, "", " ", "\t", "\n");
    }
}
