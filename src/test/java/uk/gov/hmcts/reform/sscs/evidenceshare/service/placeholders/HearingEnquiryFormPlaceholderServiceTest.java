package uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.evidenceshare.domain.FurtherEvidenceLetterType.OTHER_PARTY_LETTER;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.ADDRESS_NAME;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.APPEAL_REF;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.APPELLANT_NAME;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.BENEFIT_NAME_ACRONYM_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.CASE_ID_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.GENERATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.HMCTS2;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.HMCTS_IMG;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.OTHER_PARTIES_NAMES;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.PHONE_NUMBER;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.SSCS_URL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.SSCS_URL_LITERAL;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CcdValue;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.OtherParty;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@ExtendWith(MockitoExtension.class)
class HearingEnquiryFormPlaceholderServiceTest {

    private static final String HELPLINE_PHONE = "0800-111";
    private static final String HELPLINE_SCOTLAND_PHONE = "0800-222";

    @Mock
    private PlaceholderService placeholderService;
    @Mock
    private CcdValue<OtherParty> otherPartyCcd;

    private HearingEnquiryFormPlaceholderService service;

    @BeforeEach
    void setUp() {
        service = new HearingEnquiryFormPlaceholderService(placeholderService, HELPLINE_PHONE, HELPLINE_SCOTLAND_PHONE);
    }

    @Test
    void shouldPopulatePlaceholdersForOtherPartyIdWithPrefix() {
        final SscsCaseData caseData = buildCaseData(Benefit.CHILD_SUPPORT.getShortName());

        final Map<String, Object> placeholders = service.populatePlaceholders(caseData, OTHER_PARTY_LETTER, "otherParty111");

        assertThat(placeholders.get(ADDRESS_NAME)).isEqualTo("Other Party");
        assertThat(placeholders.get(APPELLANT_NAME)).isEqualTo("Appellant Person");
        assertThat(placeholders.get(BENEFIT_NAME_ACRONYM_LITERAL)).isEqualTo(Benefit.CHILD_SUPPORT.getDescription());
        assertThat(placeholders.get(SSCS_URL_LITERAL)).isEqualTo(SSCS_URL);
        assertThat(placeholders.get(HMCTS2)).isEqualTo(HMCTS_IMG);
        assertThat(placeholders.get(CASE_ID_LITERAL)).isEqualTo("1234567890");
        assertThat(placeholders.get(APPEAL_REF)).isEqualTo("ref-123");
        assertThat(placeholders.get(PHONE_NUMBER)).isEqualTo(HELPLINE_PHONE);
        assertThat(placeholders.get(GENERATED_DATE_LITERAL)).isNotNull();
        assertThat(placeholders.get(OTHER_PARTIES_NAMES)).isEqualTo("Second Party");
        verify(placeholderService).buildExcelaAddress(anyBoolean(), eq("No"), anyMap());
    }

    @Test
    void shouldPopulateAddressNameFromAppointeeWhenPartyIdMatchesAppointee() {
        final SscsCaseData caseData = buildCaseData(Benefit.CHILD_SUPPORT.getShortName());

        final Map<String, Object> placeholders = service.populatePlaceholders(caseData, OTHER_PARTY_LETTER, "otherParty222");

        assertThat(placeholders.get(ADDRESS_NAME)).isEqualTo("Appointee Person");
    }

    @ParameterizedTest
    @MethodSource("phoneScenarios")
    void shouldUseExpectedHelplineNumber(String isScottishCase, String expectedPhone) {
        final SscsCaseData caseData = buildCaseData(Benefit.CHILD_SUPPORT.getShortName());
        caseData.setIsScottishCase(isScottishCase);

        final Map<String, Object> placeholders = service.populatePlaceholders(caseData, OTHER_PARTY_LETTER, "otherParty111");

        assertThat(placeholders.get(PHONE_NUMBER)).isEqualTo(expectedPhone);
    }

    @Test
    void shouldUseCcdCaseIdAsAppealRefWhenCaseReferenceIsBlank() {
        final SscsCaseData caseData = buildCaseData(Benefit.CHILD_SUPPORT.getShortName());
        caseData.setCaseReference(" ");

        final Map<String, Object> placeholders = service.populatePlaceholders(caseData, OTHER_PARTY_LETTER, "otherParty111");

        assertThat(placeholders.get(APPEAL_REF)).isEqualTo("1234567890");
    }

    @Test
    void shouldUseCcdCaseIdAsAppealRefWhenCreatedInGapsFromReadyToList() {
        final SscsCaseData caseData = buildCaseData(Benefit.CHILD_SUPPORT.getShortName());
        caseData.setCreatedInGapsFrom("readyToList");

        final Map<String, Object> placeholders = service.populatePlaceholders(caseData, OTHER_PARTY_LETTER, "otherParty111");

        assertThat(placeholders.get(APPEAL_REF)).isEqualTo("1234567890");
    }

    @Test
    void shouldOmitOtherPartyWithNullNameFromOtherPartyNames() {
        final SscsCaseData caseData = buildCaseData(Benefit.CHILD_SUPPORT.getShortName());
        final List<CcdValue<OtherParty>> parties = new ArrayList<>(caseData.getOtherParties());
        parties.add(new CcdValue<>(OtherParty.builder().id("555").build()));
        caseData.setOtherParties(parties);

        final Map<String, Object> placeholders = service.populatePlaceholders(caseData, OTHER_PARTY_LETTER, "otherParty111");

        assertThat(placeholders.get(OTHER_PARTIES_NAMES)).isEqualTo("Second Party");
    }

    @Test
    void shouldOmitNullCcdValueFromOtherPartyNames() {
        SscsCaseData caseData = buildCaseData(Benefit.CHILD_SUPPORT.getShortName());
        when(otherPartyCcd.getValue()).thenReturn(null);
        final List<CcdValue<OtherParty>> parties = new ArrayList<>(caseData.getOtherParties());
        parties.add(otherPartyCcd);
        caseData.setOtherParties(parties);

        Map<String, Object> placeholders = service.populatePlaceholders(caseData, OTHER_PARTY_LETTER, "otherParty111");

        assertThat(placeholders.get(OTHER_PARTIES_NAMES)).isEqualTo("Second Party");
    }

    private static Stream<Arguments> phoneScenarios() {
        return Stream.of(Arguments.of("Yes", HELPLINE_SCOTLAND_PHONE), Arguments.of("No", HELPLINE_PHONE),
            Arguments.of(null, HELPLINE_PHONE));
    }

    private SscsCaseData buildCaseData(String benefitCode) {
        OtherParty otherParty = OtherParty.builder().id("111")
            .name(Name.builder().title("Mr").firstName("Other").lastName("Party").build())
            .address(Address.builder().line1("Other line").postcode("AB1 2CD").build()).appointee(
                Appointee.builder().id("222").name(Name.builder().title("Ms").firstName("Appointee").lastName("Person").build())
                    .address(Address.builder().line1("Appointee line").postcode("AB2 3CD").build()).build()).build();

        OtherParty secondOtherParty = OtherParty.builder().id("333")
            .name(Name.builder().title("Mrs").firstName("Second").lastName("Party").build())
            .address(Address.builder().line1("Second line").postcode("AB3 4CD").build()).appointee(
                Appointee.builder().id("444").name(Name.builder().title("Mr").firstName("Second").lastName("Appointee").build())
                    .address(Address.builder().line1("Second appointee line").postcode("AB4 5CD").build()).build()).build();

        return SscsCaseData.builder().ccdCaseId("1234567890").caseReference("ref-123").isScottishCase("No").appeal(
                Appeal.builder().benefitType(BenefitType.builder().code(benefitCode).build()).appellant(
                    Appellant.builder().name(Name.builder().title("Mr").firstName("Appellant").lastName("Person").build())
                        .address(Address.builder().line1("Appellant line").postcode("ZZ1 1ZZ").build()).build()).build())
            .otherParties(List.of(new CcdValue<>(otherParty), new CcdValue<>(secondOtherParty))).build();
    }

}
