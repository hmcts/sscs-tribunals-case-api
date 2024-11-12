package uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders;

import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType.*;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.*;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class GenericLetterPlaceholderServiceTest {

    @Mock
    PlaceholderService service;

    GenericLetterPlaceholderService genericLetterPlaceholderService;

    private SscsCaseData caseData;

    private static final String GENERIC_LETTER_TEXT = "generic_letter_text";

    @BeforeEach
    public void setup() {
        genericLetterPlaceholderService = new GenericLetterPlaceholderService(service);
        caseData = buildCaseData();
        caseData.setGenericLetterText(GENERIC_LETTER_TEXT);
    }

    @Test
    void shouldReturnAppellantPlaceholdersGivenAppellantLetter() {
        Map<String, Object> placeholders = genericLetterPlaceholderService.populatePlaceholders(caseData, APPELLANT_LETTER,
            null);

        var address = caseData.getAppeal().getAppellant().getAddress();
        String appellantName = getApellantName(caseData);

        assertNotNull(placeholders);
        assertEquals(address.getPostcode(), placeholders.get(LETTER_ADDRESS_POSTCODE));
        assertEquals(appellantName, placeholders.get(APPELLANT_NAME));
        assertEquals(appellantName, placeholders.get(NAME));
        assertEquals(appellantName, placeholders.get(ADDRESS_NAME));
        assertEquals(GENERIC_LETTER_TEXT, placeholders.get(INFO_REQUEST_DETAIL));
        assertEquals("No", placeholders.get(IS_REPRESENTATIVE));
    }

    @Test
    void shouldReturnRepresentativePlaceholdersGivenRepresentativeLetter() {
        Map<String, Object> placeholders = genericLetterPlaceholderService.populatePlaceholders(caseData, REPRESENTATIVE_LETTER,
            null);

        String appellantName = getApellantName(caseData);
        String repName = caseData.getAppeal().getRep().getName().getFullNameNoTitle();
        assertNotNull(placeholders);
        assertEquals(appellantName, placeholders.get(APPELLANT_NAME));
        assertEquals(repName, placeholders.get(NAME));
        assertEquals(repName, placeholders.get(ADDRESS_NAME));
        assertEquals("Yes", placeholders.get(IS_REPRESENTATIVE));
    }

    @Test
    void shouldReturnOtherPartyPlaceholdersGivenOtherPartyLetter() {
        OtherParty otherParty = PlaceholderHelper.buildOtherParty();
        caseData.setOtherParties(List.of(new CcdValue<>(otherParty)));

        Map<String, Object> placeholders = genericLetterPlaceholderService.populatePlaceholders(caseData, OTHER_PARTY_LETTER,
            " otherParty" + otherParty.getId());

        Address address = otherParty.getAddress();
        String appellantName = getApellantName(caseData);
        assertNotNull(placeholders);
        assertEquals(address.getPostcode(), placeholders.get(LETTER_ADDRESS_LINE_4));
        assertEquals(appellantName, placeholders.get(APPELLANT_NAME));

        String otherPartyName = otherParty.getName().getFullNameNoTitle();
        assertEquals(otherPartyName, placeholders.get(NAME));
        assertEquals(otherPartyName, placeholders.get(ADDRESS_NAME));
        assertEquals("No", placeholders.get(IS_REPRESENTATIVE));
    }

    @Test
    void shouldReturnOtherPartyRepresentativePlaceholdersGivenOtherPartyRepresentativeLetter() {
        OtherParty otherParty = PlaceholderHelper.buildOtherParty();
        Representative representative = caseData.getAppeal().getRep();

        otherParty.setRep(representative);
        caseData.setOtherParties(List.of(new CcdValue<>(otherParty)));

        String repName = representative.getName().getFullNameNoTitle();

        Map<String, Object> placeholders = genericLetterPlaceholderService.populatePlaceholders(caseData, OTHER_PARTY_REP_LETTER,
            representative.getId());

        String appellantName = getApellantName(caseData);
        assertNotNull(placeholders);
        assertEquals(appellantName, placeholders.get(APPELLANT_NAME));
        assertEquals(repName, placeholders.get(NAME));
        assertEquals(repName, placeholders.get(ADDRESS_NAME));
        assertEquals("No", placeholders.get(IS_REPRESENTATIVE));
    }

    @Test
    void shouldReturnOtherPartyAppointeePlaceholdersGivenOtherPartyHasAppointee() {
        OtherParty otherParty = PlaceholderHelper.buildOtherParty();
        Appointee appointee = caseData.getAppeal().getAppellant().getAppointee();
        otherParty.setAppointee(appointee);

        caseData.setOtherParties(List.of(new CcdValue<>(otherParty)));

        String appointeeName = appointee.getName().getFullNameNoTitle();

        Map<String, Object> placeholders = genericLetterPlaceholderService.populatePlaceholders(caseData, OTHER_PARTY_LETTER,
            appointee.getId());

        String appellantName = getApellantName(caseData);

        assertNotNull(placeholders);
        assertEquals(appellantName, placeholders.get(APPELLANT_NAME));
        assertEquals(appointeeName, placeholders.get(NAME));
        assertEquals(appointeeName, placeholders.get(ADDRESS_NAME));
        assertEquals("No", placeholders.get(IS_REPRESENTATIVE));
    }

    @Test
    void shouldReturnJointPartyPlaceholdersGivenJointPartyLetter() {
        var jointParty = PlaceholderHelper.buildJointParty();
        caseData.setJointParty(jointParty);
        String appellantName = getApellantName(caseData);
        String jointPartyName = jointParty.getName().getFullNameNoTitle();

        Map<String, Object> placeholders = genericLetterPlaceholderService.populatePlaceholders(caseData, JOINT_PARTY_LETTER,
            null);

        assertNotNull(placeholders);
        assertEquals(appellantName, placeholders.get(APPELLANT_NAME));
        assertEquals(jointPartyName, placeholders.get(NAME));
        assertEquals(jointPartyName, placeholders.get(ADDRESS_NAME));
        assertEquals("No", placeholders.get(IS_REPRESENTATIVE));
        assertEquals(JOINT, placeholders.get(JOINT));
    }

    @Test
    void shouldReturnEmptyAddressPlaceholdersGivenEmptyAddress() {
        Address emptyAddress = Address.builder().build();
        caseData.getAppeal().getAppellant().setAddress(emptyAddress);

        Map<String, Object> placeholders = genericLetterPlaceholderService.populatePlaceholders(caseData, APPELLANT_LETTER,
            null);

        assertNotNull(placeholders);
        assertFalse(placeholders.containsKey(LETTER_ADDRESS_LINE_1));
        assertFalse(placeholders.containsKey(LETTER_ADDRESS_LINE_2));
        assertFalse(placeholders.containsKey(LETTER_ADDRESS_LINE_3));
        assertFalse(placeholders.containsKey(LETTER_ADDRESS_LINE_4));
        assertFalse(placeholders.containsKey(LETTER_ADDRESS_POSTCODE));
    }

    private static String getApellantName(SscsCaseData caseData) {
        return caseData.getAppeal().getAppellant().getName().getFullNameNoTitle();
    }
}
