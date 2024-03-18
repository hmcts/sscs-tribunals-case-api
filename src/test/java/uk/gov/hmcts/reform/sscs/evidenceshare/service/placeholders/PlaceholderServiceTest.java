package uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.*;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderHelper.buildCaseData;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderHelper.buildCaseDataWithoutBenefitType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.joda.time.DateTimeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.docmosis.config.PdfDocumentConfig;
import uk.gov.hmcts.reform.sscs.evidenceshare.config.ExelaAddressConfig;

@Service
@ExtendWith(MockitoExtension.class)
public class PlaceholderServiceTest {

    PlaceholderService service;

    private SscsCaseData caseData;

    private String now;
    private String welshDate;

    @Mock
    private PdfDocumentConfig pdfDocumentConfig;

    @Mock
    private ExelaAddressConfig exelaAddressConfig;

    Map<String, Object> placeholders;

    @BeforeEach
    public void setup() {
        DateTimeUtils.setCurrentMillisFixed(1550000000000L);

        now = (DateTimeFormatter.ISO_LOCAL_DATE).format(LocalDateTime.now());
        welshDate = "2001-12-02";
        caseData = buildCaseData();
        service = new PlaceholderService(pdfDocumentConfig, exelaAddressConfig, false);
        placeholders = new HashMap<>();

        given(pdfDocumentConfig.getHmctsImgKey()).willReturn("hmctsKey");
        given(exelaAddressConfig.getAddressLine1()).willReturn("Line 1");
        given(exelaAddressConfig.getAddressLine2()).willReturn("Line 2");
        given(exelaAddressConfig.getAddressLine3()).willReturn("Line 3");
        given(exelaAddressConfig.getAddressPostcode()).willReturn("Postcode");
    }

    @Test
    public void givenACase_thenPopulateThePlaceholders() {
        Address address = Address.builder()
            .line1("Unit 2")
            .line2("156 The Road")
            .town("Lechworth")
            .county("Bedford")
            .postcode("L2 5UZ").build();

        service.build(caseData, placeholders, address, now);

        assertEquals("HM Courts & Tribunals Service", placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL));
        assertEquals("Social Security & Child Support Appeals", placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL));
        assertEquals("Prudential Buildings", placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL));
        assertEquals("36 Dale Street", placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL));
        assertEquals("LIVERPOOL", placeholders.get(REGIONAL_OFFICE_COUNTY_LITERAL));
        assertEquals("L2 5UZ", placeholders.get(REGIONAL_OFFICE_POSTCODE_LITERAL));
        assertEquals(now, placeholders.get(GENERATED_DATE_LITERAL));
        assertEquals(now, placeholders.get(CASE_CREATED_DATE_LITERAL));
        assertEquals("Mr T Tibbs", placeholders.get(APPELLANT_FULL_NAME_LITERAL));
        assertEquals("PERSONAL INDEPENDENCE PAYMENT", placeholders.get(BENEFIT_TYPE_LITERAL));
        assertEquals("123456", placeholders.get(CASE_ID_LITERAL));
        assertEquals("JT0123456B", placeholders.get(NINO_LITERAL));
        assertEquals("https://www.gov.uk/appeal-benefit-decision", placeholders.get(SSCS_URL_LITERAL));
        assertEquals("Line 1", placeholders.get(EXELA_ADDRESS_LINE1_LITERAL));
        assertEquals("Line 2", placeholders.get(EXELA_ADDRESS_LINE2_LITERAL));
        assertEquals("Line 3", placeholders.get(EXELA_ADDRESS_LINE3_LITERAL));
        assertEquals("Unit 2", placeholders.get(RECIPIENT_ADDRESS_LINE_1_LITERAL));
        assertEquals("156 The Road", placeholders.get(RECIPIENT_ADDRESS_LINE_2_LITERAL));
        assertEquals("Lechworth", placeholders.get(RECIPIENT_ADDRESS_LINE_3_LITERAL));
        assertEquals("Bedford", placeholders.get(RECIPIENT_ADDRESS_LINE_4_LITERAL));
        assertEquals("L2 5UZ", placeholders.get(RECIPIENT_ADDRESS_LINE_5_LITERAL));
        assertEquals("SC123/12/1234", placeholders.get(SC_NUMBER_LITERAL));
    }

    @Test
    public void givenACase_thenPopulateThePlaceholdersWithBenefitTypeEmpty() {
        Address address = Address.builder()
            .line1("Unit 2")
            .line2("156 The Road")
            .town("Lechworth")
            .county("Bedford")
            .postcode("L2 5UZ").build();

        caseData.getAppeal().setBenefitType(BenefitType.builder().code("PIP").description(null).build());
        service.build(caseData, placeholders, address, now);

        assertEquals("PERSONAL INDEPENDENCE PAYMENT", placeholders.get(BENEFIT_TYPE_LITERAL));
    }

    @Test
    public void givenACase_thenPopulateThePlaceholdersWithBenefitTypeDescriptionEmpty() {
        Address address = Address.builder()
            .line1("Unit 2")
            .line2("156 The Road")
            .town("Lechworth")
            .county("Bedford")
            .postcode("L2 5UZ").build();

        caseData = buildCaseDataWithoutBenefitType();
        service.build(caseData, placeholders, address, now);

        assertEquals("HM Courts & Tribunals Service", placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL));
        assertEquals("Social Security & Child Support Appeals", placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL));
        assertEquals("Prudential Buildings", placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL));
        assertEquals("36 Dale Street", placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL));
        assertEquals("LIVERPOOL", placeholders.get(REGIONAL_OFFICE_COUNTY_LITERAL));
        assertEquals("L2 5UZ", placeholders.get(REGIONAL_OFFICE_POSTCODE_LITERAL));
        assertEquals(now, placeholders.get(GENERATED_DATE_LITERAL));
        assertEquals(now, placeholders.get(CASE_CREATED_DATE_LITERAL));
        assertEquals("Mr T Tibbs", placeholders.get(APPELLANT_FULL_NAME_LITERAL));
        assertEquals("", placeholders.get(BENEFIT_TYPE_LITERAL));
        assertEquals("123456", placeholders.get(CASE_ID_LITERAL));
        assertEquals("JT0123456B", placeholders.get(NINO_LITERAL));
        assertEquals("https://www.gov.uk/appeal-benefit-decision", placeholders.get(SSCS_URL_LITERAL));
        assertEquals("Line 1", placeholders.get(EXELA_ADDRESS_LINE1_LITERAL));
        assertEquals("Line 2", placeholders.get(EXELA_ADDRESS_LINE2_LITERAL));
        assertEquals("Line 3", placeholders.get(EXELA_ADDRESS_LINE3_LITERAL));
        assertEquals("Unit 2", placeholders.get(RECIPIENT_ADDRESS_LINE_1_LITERAL));
        assertEquals("156 The Road", placeholders.get(RECIPIENT_ADDRESS_LINE_2_LITERAL));
        assertEquals("Lechworth", placeholders.get(RECIPIENT_ADDRESS_LINE_3_LITERAL));
        assertEquals("Bedford", placeholders.get(RECIPIENT_ADDRESS_LINE_4_LITERAL));
        assertEquals("L2 5UZ", placeholders.get(RECIPIENT_ADDRESS_LINE_5_LITERAL));
        assertEquals("SC123/12/1234", placeholders.get(SC_NUMBER_LITERAL));
        assertEquals("No", placeholders.get(SHOULD_HIDE_NINO));
    }

    @Test
    public void givenARecipientAddressWith4Lines_thenPopulateThePlaceholders() {
        Address address = Address.builder()
            .line1("Unit 2")
            .town("Lechworth")
            .county("Bedford")
            .postcode("L2 5UZ").build();

        service.build(caseData, placeholders, address, now);

        assertEquals("Unit 2", placeholders.get(RECIPIENT_ADDRESS_LINE_1_LITERAL));
        assertEquals("Lechworth", placeholders.get(RECIPIENT_ADDRESS_LINE_2_LITERAL));
        assertEquals("Bedford", placeholders.get(RECIPIENT_ADDRESS_LINE_3_LITERAL));
        assertEquals("L2 5UZ", placeholders.get(RECIPIENT_ADDRESS_LINE_4_LITERAL));
    }

    @Test
    public void givenAnAppellantWithALongNameAndAddressExceeding45Characters_thenGenerateThePlaceholdersWithTruncatedName() {
        Address address = Address.builder()
            .line1("MyFirstVeryVeryLongAddressLineWithLotsOfCharacters")
            .line2("MySecondVeryVeryLongAddressLineWithLotsOfCharacters")
            .town("MyTownVeryVeryLongAddressLineWithLotsOfCharacters")
            .county("MyCountyVeryVeryLongAddressLineWithLotsOfCharacters")
            .postcode("L2 5UZ").build();

        service.build(caseData, placeholders, address, now);

        assertEquals("MyFirstVeryVeryLongAddressLineWithLotsOfChara", placeholders.get(RECIPIENT_ADDRESS_LINE_1_LITERAL));
        assertEquals("MySecondVeryVeryLongAddressLineWithLotsOfChar", placeholders.get(RECIPIENT_ADDRESS_LINE_2_LITERAL));
        assertEquals("MyTownVeryVeryLongAddressLineWithLotsOfCharac", placeholders.get(RECIPIENT_ADDRESS_LINE_3_LITERAL));
        assertEquals("MyCountyVeryVeryLongAddressLineWithLotsOfChar", placeholders.get(RECIPIENT_ADDRESS_LINE_4_LITERAL));
        assertEquals("L2 5UZ", placeholders.get(RECIPIENT_ADDRESS_LINE_5_LITERAL));

    }

    @Test
    public void givenARecipientAddressWith3Lines_thenPopulateThePlaceholders() {
        Address address = Address.builder()
            .line1("Unit 2")
            .county("Bedford")
            .postcode("L2 5UZ").build();

        service.build(caseData, placeholders, address, now);

        assertEquals("Unit 2", placeholders.get(RECIPIENT_ADDRESS_LINE_1_LITERAL));
        assertEquals("Bedford", placeholders.get(RECIPIENT_ADDRESS_LINE_2_LITERAL));
        assertEquals("L2 5UZ", placeholders.get(RECIPIENT_ADDRESS_LINE_3_LITERAL));
    }

    @Test
    public void givenALanguagePreferenceIsWelsh_ThenPickWelshLogo() {
        caseData.setLanguagePreferenceWelsh("Yes");
        given(pdfDocumentConfig.getHmctsWelshImgKey()).willReturn("hmctsWelshImgKey");
        given(pdfDocumentConfig.getHmctsWelshImgVal()).willReturn("welshhmcts.png");
        Address address = Address.builder()
            .line1("Unit 2")
            .line2("156 The Road")
            .town("Lechworth")
            .county("Bedford")
            .postcode("L2 5UZ").build();

        service.build(caseData, placeholders, address, welshDate);
        assertEquals("HM Courts & Tribunals Service", placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL));
        assertEquals("welshhmcts.png", placeholders.get("hmctsWelshImgKey"));
        assertNotNull(placeholders.get(WELSH_CASE_CREATED_DATE_LITERAL));
    }

    @Test
    public void givenAChildSupportCase_thenShouldHideNino() {
        caseData.setLanguagePreferenceWelsh("Yes");
        caseData.getAppeal().getBenefitType().setCode(CHILD_SUPPORT.getShortName());
        caseData.getAppeal().getBenefitType().setDescription(CHILD_SUPPORT.getDescription());

        Address address = Address.builder()
            .line1("Unit 2")
            .line2("156 The Road")
            .town("Lechworth")
            .county("Bedford")
            .postcode("L2 5UZ").build();

        service.build(caseData, placeholders, address, welshDate);

        assertEquals("Yes", placeholders.get(SHOULD_HIDE_NINO));
    }
}
