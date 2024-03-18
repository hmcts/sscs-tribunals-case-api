package uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.YES;
import static uk.gov.hmcts.reform.sscs.ccd.util.CaseDataUtils.buildCaseData;
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
import uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class SorPlaceholderServiceTest {
    private SscsCaseData caseData;

    @Mock
    PlaceholderService placeholderService;

    SorPlaceholderService sorPlaceholderService;

    @BeforeEach
    void setup() {
        sorPlaceholderService = new SorPlaceholderService(placeholderService);
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
}
