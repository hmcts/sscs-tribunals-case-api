package uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction.GRANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction.REFUSE;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.domain.FurtherEvidenceLetterType.APPELLANT_LETTER;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.domain.FurtherEvidenceLetterType.JOINT_PARTY_LETTER;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.domain.FurtherEvidenceLetterType.OTHER_PARTY_LETTER;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.domain.FurtherEvidenceLetterType.OTHER_PARTY_REP_LETTER;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.domain.FurtherEvidenceLetterType.REPRESENTATIVE_LETTER;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.ADDRESS_NAME;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.APPELLANT_NAME;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.BENEFIT_NAME_ACRONYM_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.FIRST_TIER_AGENCY_ACRONYM;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.IBCA_URL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.INFO_REQUEST_DETAIL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.IS_REPRESENTATIVE;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.JOINT;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.LETTER_ADDRESS_LINE_1;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.LETTER_ADDRESS_LINE_2;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.LETTER_ADDRESS_LINE_3;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.LETTER_ADDRESS_LINE_4;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.LETTER_ADDRESS_POSTCODE;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.NAME;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.POSTPONEMENT_REQUEST;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.SSCS_URL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.SSCS_URL_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.gov.notify.config.AppConstants.IBC_ACRONYM;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.GenericLetterPlaceholderService;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderHelper;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderService;

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
        assertEquals(SSCS_URL, placeholders.get(SSCS_URL_LITERAL));
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

    @Test
    void shouldReturnIbcaUrlAndAcronymForIbcCase() {
        caseData.setBenefitCode("093");
        Map<String, Object> placeholders = genericLetterPlaceholderService.populatePlaceholders(caseData, APPELLANT_LETTER,
            null);

        assertNotNull(placeholders);
        assertEquals(IBC_ACRONYM, placeholders.get(BENEFIT_NAME_ACRONYM_LITERAL));
        assertEquals(IBCA_URL, placeholders.get(SSCS_URL_LITERAL));
    }

    @Test
    public void whenNotAHearingPostponementRequest_thenPlaceholderIsEmptyString() {
        Map<String, Object> placeholders = genericLetterPlaceholderService.populatePlaceholders(caseData, APPELLANT_LETTER,
            null);

        assertEquals("", placeholders.get(POSTPONEMENT_REQUEST));
    }

    @Test
    public void givenAGrantedHearingPostponementRequest_thenSetPlaceholderAccordingly() {
        caseData.setPostponementRequest(uk.gov.hmcts.reform.sscs.ccd.domain.PostponementRequest.builder().actionPostponementRequestSelected(GRANT.getValue()).build());

        Map<String, Object> placeholders = genericLetterPlaceholderService.populatePlaceholders(caseData, APPELLANT_LETTER,
            null);

        assertEquals("grant", placeholders.get(POSTPONEMENT_REQUEST));
    }

    @Test
    public void givenARefusedHearingPostponementRequest_thenSetPlaceholderAccordingly() {
        caseData.setPostponementRequest(uk.gov.hmcts.reform.sscs.ccd.domain.PostponementRequest.builder().actionPostponementRequestSelected(REFUSE.getValue()).build());

        Map<String, Object> placeholders = genericLetterPlaceholderService.populatePlaceholders(caseData, APPELLANT_LETTER,
            null);

        assertEquals("refuse", placeholders.get(POSTPONEMENT_REQUEST));
    }

    @ParameterizedTest
    @CsvSource({
        "ESA, DWP",
        "JSA, DWP",
        "PIP, DWP",
        "DLA, DWP",
        "UC, DWP",
        "CARERS_ALLOWANCE, DWP",
        "ATTENDANCE_ALLOWANCE, DWP",
        "BEREAVEMENT_BENEFIT, DWP",
        "IIDB, DWP",
        "MATERNITY_ALLOWANCE, DWP",
        "SOCIAL_FUND, DWP",
        "INCOME_SUPPORT, DWP",
        "BEREAVEMENT_SUPPORT_PAYMENT_SCHEME, DWP",
        "INDUSTRIAL_DEATH_BENEFIT, DWP",
        "PENSION_CREDIT, DWP",
        "RETIREMENT_PENSION, DWP",
        "CHILD_SUPPORT, DWP",
        "TAX_CREDIT, HMRC",
        "GUARDIANS_ALLOWANCE, HMRC",
        "TAX_FREE_CHILDCARE, HMRC",
        "HOME_RESPONSIBILITIES_PROTECTION, HMRC",
        "CHILD_BENEFIT, HMRC",
        "THIRTY_HOURS_FREE_CHILDCARE, HMRC",
        "GUARANTEED_MINIMUM_PENSION, HMRC",
        "NATIONAL_INSURANCE_CREDITS, HMRC",
        "INFECTED_BLOOD_COMPENSATION, IBCA"
    })
    void shouldSetFtaAcronymCorrectly(Benefit benefit, String ftaAcronym) {
        caseData.getAppeal().setBenefitType(BenefitType.builder()
            .code(benefit.getShortName())
            .description(benefit.getDescription())
            .build());
        Map<String, Object> placeholders = genericLetterPlaceholderService.populatePlaceholders(caseData, APPELLANT_LETTER,
            null);
        assertEquals(ftaAcronym, placeholders.get(FIRST_TIER_AGENCY_ACRONYM));
    }


    private static String getApellantName(SscsCaseData caseData) {
        return caseData.getAppeal().getAppellant().getName().getFullNameNoTitle();
    }
}
