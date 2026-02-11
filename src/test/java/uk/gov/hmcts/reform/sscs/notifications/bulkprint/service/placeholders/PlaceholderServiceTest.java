package uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.BDDMockito.given;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.TAX_CREDIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction.GRANT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.ProcessRequestAction.REFUSE;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.APPELLANT_FULL_NAME_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.BENEFIT_NAME_ACRONYM_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.BENEFIT_NAME_ACRONYM_LITERAL_WELSH;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.BENEFIT_TYPE_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.CASE_CREATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.CASE_ID_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_LINE1_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_LINE2_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_LINE3_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_POSTCODE_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.FIRST_TIER_AGENCY_ACRONYM;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.GENERATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.IBCA_URL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.LABEL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.NINO_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.POSTPONEMENT_REQUEST;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_1_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_2_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_3_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_4_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_5_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_COUNTY_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_POSTCODE_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.SC_NUMBER_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.SHOULD_HIDE_NINO;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.SSCS_URL_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderConstants.WELSH_CASE_CREATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderHelper.buildCaseData;
import static uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderHelper.buildCaseDataWithoutBenefitType;
import static uk.gov.hmcts.reform.sscs.model.AppConstants.IBCA_BENEFIT_CODE;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.PostponementRequest;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.thirdparty.docmosis.config.PdfDocumentConfig;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.config.ExelaAddressConfig;
import uk.gov.hmcts.reform.sscs.notifications.bulkprint.service.placeholders.PlaceholderService;

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
        now = (DateTimeFormatter.ISO_LOCAL_DATE).format(LocalDate.now());
        welshDate = "2001-12-02";
        caseData = buildCaseData();
        service = new PlaceholderService(pdfDocumentConfig, exelaAddressConfig, false);
        placeholders = new HashMap<>();

        given(pdfDocumentConfig.getHmctsImgKey()).willReturn("hmctsKey");
    }

    @Test
    public void givenACase_thenPopulateThePlaceholders() {
        setAddressMocks();
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
        assertEquals("PIP", placeholders.get(BENEFIT_NAME_ACRONYM_LITERAL));
        assertEquals("PIP", placeholders.get(BENEFIT_NAME_ACRONYM_LITERAL_WELSH));
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
        setAddressMocks();
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
        setAddressMocks();
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
        assertEquals("NI No", placeholders.get(LABEL));
        assertNull(placeholders.get(BENEFIT_NAME_ACRONYM_LITERAL));
        assertNull(placeholders.get(BENEFIT_NAME_ACRONYM_LITERAL_WELSH));
    }

    @Test
    public void givenARecipientAddressWith4Lines_thenPopulateThePlaceholders() {
        setAddressMocks();
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
        setAddressMocks();
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
        setAddressMocks();
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
        setAddressMocks();
        caseData.setLanguagePreferenceWelsh("Yes");
        given(pdfDocumentConfig.getHmctsWelshImgKey()).willReturn("hmctsWelshImgKey");
        given(pdfDocumentConfig.getHmctsWelshImgVal()).willReturn("welshhmcts.png");
        caseData.getAppeal().getBenefitType().setCode(TAX_CREDIT.getShortName());
        caseData.getAppeal().getBenefitType().setDescription(TAX_CREDIT.getDescription());
        Address address = Address.builder()
            .line1("Unit 2")
            .line2("156 The Road")
            .town("Lechworth")
            .county("Bedford")
            .postcode("L2 5UZ").build();

        service.build(caseData, placeholders, address, welshDate);
        assertEquals("HM Courts & Tribunals Service", placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL));
        assertEquals("welshhmcts.png", placeholders.get("hmctsWelshImgKey"));
        assertEquals("HMRC", placeholders.get(FIRST_TIER_AGENCY_ACRONYM));
        assertNotNull(placeholders.get(WELSH_CASE_CREATED_DATE_LITERAL));
    }

    @Test
    public void givenAChildSupportCase_thenShouldHideNino() {
        setAddressMocks();
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
        assertEquals("DWP", placeholders.get(FIRST_TIER_AGENCY_ACRONYM));
        assertEquals("Yes", placeholders.get(SHOULD_HIDE_NINO));
    }

    @Test
    public void givenAnIbcaCase_thenPopulateThePlaceholders() {
        given(exelaAddressConfig.getIbcAddressLine1()).willReturn("IBC line 1");
        given(exelaAddressConfig.getIbcAddressLine2()).willReturn("IBC line 2");
        given(exelaAddressConfig.getIbcAddressPostcode()).willReturn("IBC postcode");
        given(exelaAddressConfig.getAddressLine3()).willReturn("Line 3");
        caseData.setBenefitCode(IBCA_BENEFIT_CODE);
        caseData.getAppeal().setBenefitType(BenefitType.builder()
            .code("infectedBloodCompensation")
            .description("Infected Blood Compensation")
            .build()
        );
        caseData.getAppeal().getAppellant().setIdentity(Identity.builder().ibcaReference("E24D55").build());

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
        assertEquals("INFECTED BLOOD COMPENSATION", placeholders.get(BENEFIT_TYPE_LITERAL));
        assertEquals("IBC", placeholders.get(BENEFIT_NAME_ACRONYM_LITERAL));
        assertEquals("IGH", placeholders.get(BENEFIT_NAME_ACRONYM_LITERAL_WELSH));
        assertEquals("IBCA", placeholders.get(FIRST_TIER_AGENCY_ACRONYM));
        assertEquals("123456", placeholders.get(CASE_ID_LITERAL));
        assertEquals(IBCA_URL, placeholders.get(SSCS_URL_LITERAL));
        assertEquals("IBC line 1", placeholders.get(EXELA_ADDRESS_LINE1_LITERAL));
        assertEquals("IBC line 2", placeholders.get(EXELA_ADDRESS_LINE2_LITERAL));
        assertEquals("Line 3", placeholders.get(EXELA_ADDRESS_LINE3_LITERAL));
        assertEquals("IBC postcode", placeholders.get(EXELA_ADDRESS_POSTCODE_LITERAL));
        assertEquals("Unit 2", placeholders.get(RECIPIENT_ADDRESS_LINE_1_LITERAL));
        assertEquals("156 The Road", placeholders.get(RECIPIENT_ADDRESS_LINE_2_LITERAL));
        assertEquals("Lechworth", placeholders.get(RECIPIENT_ADDRESS_LINE_3_LITERAL));
        assertEquals("Bedford", placeholders.get(RECIPIENT_ADDRESS_LINE_4_LITERAL));
        assertEquals("L2 5UZ", placeholders.get(RECIPIENT_ADDRESS_LINE_5_LITERAL));
        assertEquals("No", placeholders.get(SHOULD_HIDE_NINO));
        assertEquals("E24D55", placeholders.get(NINO_LITERAL));
        assertEquals("IBCA Reference", placeholders.get(LABEL));
    }

    @Test
    public void givenAScottishCase_thenPopulateThePlaceholders() {
        service = new PlaceholderService(pdfDocumentConfig, exelaAddressConfig, true);
        given(exelaAddressConfig.getScottishAddressLine2()).willReturn("Scottish line 2");
        given(exelaAddressConfig.getScottishPostcode()).willReturn("Scottish postcode");
        given(exelaAddressConfig.getAddressLine1()).willReturn("Line 1");
        given(exelaAddressConfig.getAddressLine3()).willReturn("Line 3");
        caseData.setIsScottishCase("Yes");

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
        assertEquals("Scottish line 2", placeholders.get(EXELA_ADDRESS_LINE2_LITERAL));
        assertEquals("Scottish postcode", placeholders.get(EXELA_ADDRESS_POSTCODE_LITERAL));
        assertEquals("Line 3", placeholders.get(EXELA_ADDRESS_LINE3_LITERAL));
        assertEquals("Unit 2", placeholders.get(RECIPIENT_ADDRESS_LINE_1_LITERAL));
        assertEquals("156 The Road", placeholders.get(RECIPIENT_ADDRESS_LINE_2_LITERAL));
        assertEquals("Lechworth", placeholders.get(RECIPIENT_ADDRESS_LINE_3_LITERAL));
        assertEquals("Bedford", placeholders.get(RECIPIENT_ADDRESS_LINE_4_LITERAL));
        assertEquals("L2 5UZ", placeholders.get(RECIPIENT_ADDRESS_LINE_5_LITERAL));
        assertEquals("SC123/12/1234", placeholders.get(SC_NUMBER_LITERAL));
    }

    @Test
    public void whenNotAHearingPostponementRequest_thenPlaceholderIsEmptyString() {
        Address address = Address.builder()
            .line1("Unit 2")
            .line2("156 The Road")
            .town("Lechworth")
            .county("Bedford")
            .postcode("L2 5UZ").build();

        service.build(caseData, placeholders, address, now);

        assertEquals("", placeholders.get(POSTPONEMENT_REQUEST));
    }

    @Test
    public void givenAGrantedHearingPostponementRequest_thenSetPlaceholderAccordingly() {
        caseData.setPostponementRequest(PostponementRequest.builder().actionPostponementRequestSelected(GRANT.getValue()).build());
        Address address = Address.builder()
            .line1("Unit 2")
            .line2("156 The Road")
            .town("Lechworth")
            .county("Bedford")
            .postcode("L2 5UZ").build();

        service.build(caseData, placeholders, address, now);
        assertEquals("grant", placeholders.get(POSTPONEMENT_REQUEST));
    }

    @Test
    public void givenARefusedHearingPostponementRequest_thenSetPlaceholderAccordingly() {
        caseData.setPostponementRequest(PostponementRequest.builder().actionPostponementRequestSelected(REFUSE.getValue()).build());

        Address address = Address.builder()
            .line1("Unit 2")
            .line2("156 The Road")
            .town("Lechworth")
            .county("Bedford")
            .postcode("L2 5UZ").build();

        service.build(caseData, placeholders, address, now);
        assertEquals("DWP", placeholders.get(FIRST_TIER_AGENCY_ACRONYM));
        assertEquals("refuse", placeholders.get(POSTPONEMENT_REQUEST));
    }

    private void setAddressMocks() {
        given(exelaAddressConfig.getAddressLine1()).willReturn("Line 1");
        given(exelaAddressConfig.getAddressLine2()).willReturn("Line 2");
        given(exelaAddressConfig.getAddressLine3()).willReturn("Line 3");
        given(exelaAddressConfig.getAddressPostcode()).willReturn("Postcode");
    }
}
