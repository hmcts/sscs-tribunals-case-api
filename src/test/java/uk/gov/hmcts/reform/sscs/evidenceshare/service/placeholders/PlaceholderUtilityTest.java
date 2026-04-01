package uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction.GRANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction.REFUSE;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType.APPELLANT_LETTER;
import static uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType.OTHER_PARTY_LETTER;
import static uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType.OTHER_PARTY_REP_LETTER;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderUtility.getPostponementRequestStatus;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public class PlaceholderUtilityTest {
    private SscsCaseData caseData = buildCaseData();

    @Test
    public void whenNotAHearingPostponementRequest_thenGetPostponementRequestStatusReturnEmptyString() {
        String response = getPostponementRequestStatus(caseData);

        assertEquals("", getPostponementRequestStatus(caseData));
    }

    @Test
    public void givenAGrantedHearingPostponementRequest_GetPostponementRequestStatusReturnGrant() {
        caseData.setPostponementRequest(uk.gov.hmcts.reform.sscs.ccd.domain.PostponementRequest.builder().actionPostponementRequestSelected(GRANT.getValue()).build());

        assertEquals("grant", getPostponementRequestStatus(caseData));
    }

    @Test
    public void givenARefusedHearingPostponementRequest_GetPostponementRequestStatusReturnRefuse() {
        caseData.setPostponementRequest(uk.gov.hmcts.reform.sscs.ccd.domain.PostponementRequest.builder().actionPostponementRequestSelected(REFUSE.getValue()).build());

        assertEquals("refuse", getPostponementRequestStatus(caseData));
    }

    @Test
    public void isValidLetterAddress_returnsTrueForValidAddress() {
        Address address = Address.builder().line1("10 Downing Street").postcode("SW1A 2AA").build();
        assertTrue(PlaceholderUtility.isValidLetterAddress(address));
    }

    @Test
    public void isValidLetterAddress_returnsFalseForNullAddress() {
        assertFalse(PlaceholderUtility.isValidLetterAddress(null));
    }

    @Test
    public void isValidLetterAddress_returnsFalseForMissingLine1() {
        Address address = Address.builder().postcode("SW1A 2AA").build();
        assertFalse(PlaceholderUtility.isValidLetterAddress(address));
    }

    @Test
    public void isValidLetterAddress_returnsFalseForMissingPostcode() {
        Address address = Address.builder().line1("10 Downing Street").build();
        assertFalse(PlaceholderUtility.isValidLetterAddress(address));
    }

    @Test
    public void isValidLetterAddress_returnsFalseForBlankLine1() {
        Address address = Address.builder().line1("").postcode("SW1A 2AA").build();
        assertFalse(PlaceholderUtility.isValidLetterAddress(address));
    }

    @Test
    public void getName_returnsSirMadamWhenOtherPartyIdDoesNotMatch() {
        SscsCaseData data = buildCaseDataWithOtherParty();

        String name = PlaceholderUtility.getName(data, OTHER_PARTY_LETTER, "non-existent-id");

        assertEquals("Sir/Madam", name);
    }

    @Test
    public void getName_returnsOtherPartyNameWhenIdMatches() {
        SscsCaseData data = buildCaseDataWithOtherParty();

        String name = PlaceholderUtility.getName(data, OTHER_PARTY_LETTER, "op-uuid-1");

        assertEquals("Other Party", name);
    }

    @Test
    public void getName_returnsOtherPartyRepNameWhenRepIdMatches() {
        SscsCaseData data = buildCaseDataWithOtherParty();

        String name = PlaceholderUtility.getName(data, OTHER_PARTY_REP_LETTER, "rep-uuid-1");

        assertEquals("Rep Person", name);
    }

    @Test
    public void getName_returnsSirMadamWhenOtherPartiesIsNull() {
        SscsCaseData data = buildCaseData();
        data.setOtherParties(null);

        String name = PlaceholderUtility.getName(data, OTHER_PARTY_LETTER, "some-id");

        assertEquals("Sir/Madam", name);
    }

    @Test
    public void getAddress_returnsEmptyAddressWhenOtherPartyIdDoesNotMatch() {
        SscsCaseData data = buildCaseDataWithOtherParty();

        Address address = PlaceholderUtility.getAddress(data, OTHER_PARTY_LETTER, "non-existent-id");

        assertEquals("", address.getLine1());
        assertEquals("", address.getPostcode());
    }

    @Test
    public void getAddress_returnsOtherPartyAddressWhenIdMatches() {
        SscsCaseData data = buildCaseDataWithOtherParty();

        Address address = PlaceholderUtility.getAddress(data, OTHER_PARTY_LETTER, "op-uuid-1");

        assertEquals("10 Other Street", address.getLine1());
        assertEquals("OP1 1AA", address.getPostcode());
    }

    @Test
    public void getAddress_returnsEmptyAddressWhenOtherPartiesIsNull() {
        SscsCaseData data = buildCaseData();
        data.setOtherParties(null);

        Address address = PlaceholderUtility.getAddress(data, OTHER_PARTY_LETTER, "some-id");

        assertEquals("", address.getLine1());
    }

    @Test
    public void getAddress_returnsAppellantAddressForAppellantLetter() {
        Address address = PlaceholderUtility.getAddress(caseData, APPELLANT_LETTER, null);

        assertEquals(caseData.getAppeal().getAppellant().getAddress().getLine1(), address.getLine1());
    }

    @Test
    public void getAddress_returnsAppointeeAddressWhenIsAppointeeYes() {
        Address appointeeAddress = Address.builder().line1("Appointee Street").postcode("AP1 1AA").build();
        caseData.getAppeal().getAppellant().setIsAppointee("Yes");
        caseData.getAppeal().getAppellant().setAppointee(
            Appointee.builder()
                .name(Name.builder().firstName("App").lastName("Ointee").build())
                .address(appointeeAddress)
                .build());

        Address address = PlaceholderUtility.getAddress(caseData, APPELLANT_LETTER, null);

        assertEquals("Appointee Street", address.getLine1());
    }

    private SscsCaseData buildCaseDataWithOtherParty() {
        SscsCaseData data = buildCaseData();
        data.setOtherParties(List.of(new CcdValue<>(
            OtherParty.builder()
                .id("op-uuid-1")
                .name(Name.builder().firstName("Other").lastName("Party").build())
                .address(Address.builder().line1("10 Other Street").postcode("OP1 1AA").build())
                .rep(Representative.builder()
                    .id("rep-uuid-1")
                    .name(Name.builder().firstName("Rep").lastName("Person").build())
                    .address(Address.builder().line1("20 Rep Street").postcode("RP1 1AA").build())
                    .hasRepresentative("Yes")
                    .build())
                .build())));
        return data;
    }
}