package uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction.GRANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction.REFUSE;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.YES;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.ADDRESS_NAME;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.APPELLANT_NAME;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.ENTITY_TYPE;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.HMC_HEARING_TYPE_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.LETTER_ADDRESS_LINE_1;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.LETTER_ADDRESS_LINE_2;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.LETTER_ADDRESS_LINE_4;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.LETTER_ADDRESS_POSTCODE;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.NAME;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.PHONE_NUMBER;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.POSTPONEMENT_REQUEST;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.domain.FurtherEvidenceLetterType;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderHelper;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderService;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.SorPlaceholderService;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class SorPlaceholderServiceTest {
    private SscsCaseData caseData;

    @Mock
    PlaceholderService placeholderService;

    SorPlaceholderService sorPlaceholderService;

    @BeforeEach
    void setup() {
        sorPlaceholderService = new SorPlaceholderService(placeholderService,
            "0300 123 1142",
            "0300 131 2850",
            "0300 790 6234");
        caseData = buildCaseData();
    }

    @Test
    void caseDataNull() {
        assertThrows(NullPointerException.class, () ->
            sorPlaceholderService.populatePlaceholders(null, null, null, null));
    }

    @Test
    void returnAppellantAddressAndNamePlaceholdersGivenAppellantLetter() {
        var placeholders = sorPlaceholderService.populatePlaceholders(caseData, FurtherEvidenceLetterType.APPELLANT_LETTER,
            Appellant.class.getSimpleName(), null);

        var appellantAddress = caseData.getAppeal().getAppellant().getAddress();
        assertEquals(Appellant.class.getSimpleName(), placeholders.get(ENTITY_TYPE));
        assertEquals(appellantAddress.getLine1(), placeholders.get(LETTER_ADDRESS_LINE_1));
        assertEquals(appellantAddress.getLine2(), placeholders.get(LETTER_ADDRESS_LINE_2));
        assertEquals(appellantAddress.getPostcode(), placeholders.get(LETTER_ADDRESS_POSTCODE));

        var appellantName = caseData.getAppeal().getAppellant().getName().getFullNameNoTitle();
        assertEquals(appellantName, placeholders.get(ADDRESS_NAME));
        assertEquals(appellantName, placeholders.get(NAME));
        assertEquals(appellantName, placeholders.get(APPELLANT_NAME));
    }

    @Test
    void returnRepresentativeAddressAndNamePlaceholdersGivenRepresentativeLetter() {
        var placeholders = sorPlaceholderService.populatePlaceholders(caseData, FurtherEvidenceLetterType.REPRESENTATIVE_LETTER,
            Representative.class.getSimpleName(), null);

        var representativeAddress = caseData.getAppeal().getRep().getAddress();
        assertEquals(Representative.class.getSimpleName(), placeholders.get(ENTITY_TYPE));
        assertEquals(representativeAddress.getLine1(), placeholders.get(LETTER_ADDRESS_LINE_1));
        assertEquals(representativeAddress.getLine2(), placeholders.get(LETTER_ADDRESS_LINE_2));
        assertEquals(representativeAddress.getPostcode(), placeholders.get(LETTER_ADDRESS_POSTCODE));

        var representativeName = caseData.getAppeal().getRep().getName().getFullNameNoTitle();
        assertEquals(representativeName, placeholders.get(ADDRESS_NAME));
        assertEquals(representativeName, placeholders.get(NAME));

        var appellantName = caseData.getAppeal().getAppellant().getName().getFullNameNoTitle();
        assertEquals(appellantName, placeholders.get(APPELLANT_NAME));
    }

    @Test
    void returnOtherPartyAddressAndNamePlaceholdersGivenOtherPartyLetter() {
        OtherParty otherParty = PlaceholderHelper.buildOtherParty();
        caseData.setOtherParties(List.of(new CcdValue<>(otherParty)));

        var placeholders = sorPlaceholderService.populatePlaceholders(caseData, FurtherEvidenceLetterType.OTHER_PARTY_LETTER,
            OtherParty.class.getSimpleName(), " otherParty" + otherParty.getId());

        var otherPartyAddress = otherParty.getAddress();
        assertEquals(OtherParty.class.getSimpleName(), placeholders.get(ENTITY_TYPE));
        assertEquals(otherPartyAddress.getLine1(), placeholders.get(LETTER_ADDRESS_LINE_1));
        assertEquals(otherPartyAddress.getLine2(), placeholders.get(LETTER_ADDRESS_LINE_2));
        assertEquals(otherPartyAddress.getPostcode(), placeholders.get(LETTER_ADDRESS_LINE_4));

        var otherPartyName = otherParty.getName().getFullNameNoTitle();
        assertEquals(otherPartyName, placeholders.get(ADDRESS_NAME));
        assertEquals(otherPartyName, placeholders.get(NAME));

        var appellantName = caseData.getAppeal().getAppellant().getName().getFullNameNoTitle();
        assertEquals(appellantName, placeholders.get(APPELLANT_NAME));
    }

    @Test
    void returnJointPartyAddressAndNamePlaceholdersGivenJointPartyLetter() {
        var jointParty = PlaceholderHelper.buildJointParty();
        caseData.setJointParty(jointParty);

        Map<String, Object> placeholders = sorPlaceholderService.populatePlaceholders(caseData, FurtherEvidenceLetterType.JOINT_PARTY_LETTER,
            JointParty.class.getSimpleName(), null);

        var jointPartyAddress = caseData.getJointParty().getAddress();
        assertEquals(JointParty.class.getSimpleName(), placeholders.get(ENTITY_TYPE));
        assertEquals(jointPartyAddress.getLine1(), placeholders.get(LETTER_ADDRESS_LINE_1));
        assertEquals(jointPartyAddress.getLine2(), placeholders.get(LETTER_ADDRESS_LINE_2));
        assertEquals(jointPartyAddress.getPostcode(), placeholders.get(LETTER_ADDRESS_LINE_4));

        var jointPartyName = caseData.getJointParty().getName().getFullNameNoTitle();
        assertEquals(jointPartyName, placeholders.get(ADDRESS_NAME));
        assertEquals(jointPartyName, placeholders.get(NAME));

        var appellantName = caseData.getAppeal().getAppellant().getName().getFullNameNoTitle();
        assertEquals(appellantName, placeholders.get(APPELLANT_NAME));
    }

    @Test
    void returnAppointeeAddressAndNamePlaceholdersGivenAppointeeLetter() {
        caseData.getAppeal().getAppellant().setIsAppointee(YES);
        var placeholders = sorPlaceholderService.populatePlaceholders(caseData, FurtherEvidenceLetterType.APPELLANT_LETTER,
            Appointee.class.getSimpleName(), null);

        var appointeeAddress = caseData.getAppeal().getAppellant().getAppointee().getAddress();
        assertEquals(Appointee.class.getSimpleName(), placeholders.get(ENTITY_TYPE));
        assertEquals(appointeeAddress.getLine1(), placeholders.get(LETTER_ADDRESS_LINE_1));
        assertEquals(appointeeAddress.getLine2(), placeholders.get(LETTER_ADDRESS_LINE_2));
        assertEquals(appointeeAddress.getPostcode(), placeholders.get(LETTER_ADDRESS_POSTCODE));

        var appointeeName = caseData.getAppeal().getAppellant().getAppointee().getName().getFullNameNoTitle();
        assertEquals(appointeeName, placeholders.get(ADDRESS_NAME));
        assertEquals(appointeeName, placeholders.get(NAME));

        var appellantName = caseData.getAppeal().getAppellant().getName().getFullNameNoTitle();
        assertEquals(appellantName, placeholders.get(APPELLANT_NAME));
    }

    @Test
    void whenNotAHearingPostponementRequest_thenPlaceholderIsEmptyString() {
        Map<String, Object> placeholders = sorPlaceholderService.populatePlaceholders(caseData, FurtherEvidenceLetterType.APPELLANT_LETTER,
            Appointee.class.getSimpleName(), null);

        assertEquals("", placeholders.get(POSTPONEMENT_REQUEST));
    }

    @Test
    void givenAGrantedHearingPostponementRequest_thenSetPlaceholderAccordingly() {
        caseData.setPostponementRequest(uk.gov.hmcts.reform.sscs.ccd.domain.PostponementRequest.builder().actionPostponementRequestSelected(GRANT.getValue()).build());

        Map<String, Object> placeholders = sorPlaceholderService.populatePlaceholders(caseData, FurtherEvidenceLetterType.APPELLANT_LETTER,
            Appointee.class.getSimpleName(), null);

        assertEquals("grant", placeholders.get(POSTPONEMENT_REQUEST));
    }

    @Test
    void givenARefusedHearingPostponementRequest_thenSetPlaceholderAccordingly() {
        caseData.setPostponementRequest(uk.gov.hmcts.reform.sscs.ccd.domain.PostponementRequest.builder().actionPostponementRequestSelected(REFUSE.getValue()).build());

        Map<String, Object> placeholders = sorPlaceholderService.populatePlaceholders(caseData, FurtherEvidenceLetterType.APPELLANT_LETTER,
            Appointee.class.getSimpleName(), null);

        assertEquals("refuse", placeholders.get(POSTPONEMENT_REQUEST));
    }

    @ParameterizedTest
    @EnumSource(value = HmcHearingType.class, names = {"DIRECTION_HEARINGS", "SUBSTANTIVE"})
    void shouldReturnDirectionHearingPlaceholder(HmcHearingType hmcHearingType) {
        caseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder().overrideFields(OverrideFields.builder().hmcHearingType(hmcHearingType).build()).build());
        var placeholders = sorPlaceholderService.populatePlaceholders(caseData, FurtherEvidenceLetterType.APPELLANT_LETTER,
            Appointee.class.getSimpleName(), null);

        assertEquals(hmcHearingType.getHmcReference(), placeholders.get(HMC_HEARING_TYPE_LITERAL));
    }

    @Test
    void shouldReturnSubstantiveDueToNullHearingPlaceholder() {
        caseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder().overrideFields(OverrideFields.builder().hmcHearingType(null).build()).build());
        var placeholders = sorPlaceholderService.populatePlaceholders(caseData, FurtherEvidenceLetterType.APPELLANT_LETTER,
            Appointee.class.getSimpleName(), null);

        assertEquals("BBA3-SUB", placeholders.get(HMC_HEARING_TYPE_LITERAL));
        assertEquals("0300 123 1142", placeholders.get(PHONE_NUMBER));
    }

    @ParameterizedTest
    @CsvSource(value = {"093,Yes,0300 790 6234", "093,No,0300 131 2850", "001,No,0300 123 1142"})
    void shouldReturnCorrectPhoneNumberIfNullRpc(String benefitCode, String yesNo, String phoneNumber) {
        caseData.setBenefitCode(benefitCode);
        caseData.setIsScottishCase(yesNo);
        caseData.setSchedulingAndListingFields(SchedulingAndListingFields.builder().overrideFields(OverrideFields.builder().hmcHearingType(null).build()).build());
        caseData.setRegionalProcessingCenter(null);
        var placeholders = sorPlaceholderService.populatePlaceholders(caseData, FurtherEvidenceLetterType.APPELLANT_LETTER,
            Appointee.class.getSimpleName(), null);

        assertEquals(phoneNumber, placeholders.get(PHONE_NUMBER));
    }
}
