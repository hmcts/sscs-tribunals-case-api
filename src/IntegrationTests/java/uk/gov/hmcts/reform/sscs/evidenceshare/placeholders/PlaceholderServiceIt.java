package uk.gov.hmcts.reform.sscs.evidenceshare.placeholders;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.APPELLANT_FULL_NAME_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.BENEFIT_TYPE_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.CASE_CREATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.CASE_ID_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_LINE1_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_LINE2_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_LINE3_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.EXELA_ADDRESS_POSTCODE_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.GENERATED_DATE_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.NINO_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_1_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_2_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_3_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.RECIPIENT_ADDRESS_LINE_4_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_COUNTY_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_PHONE_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.REGIONAL_OFFICE_POSTCODE_LITERAL;
import static uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderConstants.SSCS_URL_LITERAL;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.evidenceshare.service.placeholders.PlaceholderService;
import uk.gov.hmcts.reform.sscs.service.AirLookupService;


@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:config/application_es_it.properties")
@ActiveProfiles("integration")
public class PlaceholderServiceIt {

    private static final String RPC_ADDRESS1 = "HM Courts & Tribunals Service";
    private static final String RPC_ADDRESS2 = "Social Security & Child Support Appeals";
    private static final String RPC_ADDRESS3 = "Prudential Buildings";
    private static final String RPC_ADDRESS4 = "36 Dale Street";
    private static final String RPC_CITY = "LIVERPOOL";
    private static final String POSTCODE = "L2 5UZ";
    private static final String PHONE = "0123456789";

    private SscsCaseData caseData;
    private String now;

    @Autowired
    private PlaceholderService placeholderService;

    @MockitoBean
    protected AirLookupService airLookupService;


    @Before
    public void setup() {
        now = DateTimeFormatter.ISO_LOCAL_DATE.format(LocalDate.now());
    }

    @Test
    public void givenACaseData_thenGenerateThePlaceholderMappings() {
        Address address = Address.builder().line1("123 The Road").town("Brentwood").postcode("CM12 1TH").build();

        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
            .name("Liverpool").address1(RPC_ADDRESS1).address2(RPC_ADDRESS2).address3(RPC_ADDRESS3)
            .address4(RPC_ADDRESS4).city(RPC_CITY).postcode(POSTCODE).phoneNumber(PHONE).build();

        Map<String, Object> placeholders = new HashMap<>();
        caseData = buildCaseData(rpc);
        placeholderService.build(caseData, placeholders, address, now);

        assertEquals(now, placeholders.get(CASE_CREATED_DATE_LITERAL));
        assertEquals(RPC_ADDRESS1, placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL));
        assertEquals(RPC_ADDRESS2, placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL));
        assertEquals(RPC_ADDRESS3, placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL));
        assertEquals(RPC_ADDRESS4, placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL));
        assertEquals(RPC_CITY, placeholders.get(REGIONAL_OFFICE_COUNTY_LITERAL));
        assertEquals(POSTCODE, placeholders.get(REGIONAL_OFFICE_POSTCODE_LITERAL));
        assertEquals(PHONE, placeholders.get(REGIONAL_OFFICE_PHONE_LITERAL));
        assertEquals("123 The Road", placeholders.get(RECIPIENT_ADDRESS_LINE_1_LITERAL));
        assertEquals("Brentwood", placeholders.get(RECIPIENT_ADDRESS_LINE_2_LITERAL));
        assertEquals("CM12 1TH", placeholders.get(RECIPIENT_ADDRESS_LINE_3_LITERAL));
        assertEquals(now, placeholders.get(GENERATED_DATE_LITERAL));
        assertEquals("Mr T Tibbs", placeholders.get(APPELLANT_FULL_NAME_LITERAL));
        assertEquals("PERSONAL INDEPENDENCE PAYMENT", placeholders.get(BENEFIT_TYPE_LITERAL));
        assertEquals("123456", placeholders.get(CASE_ID_LITERAL));
        assertEquals("JT0123456B", placeholders.get(NINO_LITERAL));
        assertEquals("https://www.gov.uk/appeal-benefit-decision", placeholders.get(SSCS_URL_LITERAL));
        assertEquals("HMCTS SSCS", placeholders.get(EXELA_ADDRESS_LINE1_LITERAL));
        assertEquals("PO BOX 12626", placeholders.get(EXELA_ADDRESS_LINE2_LITERAL));
        assertEquals("Harlow", placeholders.get(EXELA_ADDRESS_LINE3_LITERAL));
        assertEquals("CM20 9QF", placeholders.get(EXELA_ADDRESS_POSTCODE_LITERAL));
    }

    @Test
    public void givenAScottishCaseAndPoBoxFeatureOn_thenSetExcelaAddressCorrectly() {
        ReflectionTestUtils.setField(placeholderService, "scottishPoBoxEnabled", true);

        Address address = Address.builder().line1("123 The Road").town("Brentwood").postcode("CM12 1TH").build();

        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
            .name("Liverpool").address1(RPC_ADDRESS1).address2(RPC_ADDRESS2).address3(RPC_ADDRESS3)
            .address4(RPC_ADDRESS4).city(RPC_CITY).postcode(POSTCODE).phoneNumber(PHONE).build();

        Map<String, Object> placeholders = new HashMap<>();
        caseData = buildCaseData(rpc);
        caseData.setIsScottishCase("Yes");
        placeholderService.build(caseData, placeholders, address, now);

        assertEquals("HMCTS SSCS", placeholders.get(EXELA_ADDRESS_LINE1_LITERAL));
        assertEquals("PO BOX 13150", placeholders.get(EXELA_ADDRESS_LINE2_LITERAL));
        assertEquals("Harlow", placeholders.get(EXELA_ADDRESS_LINE3_LITERAL));
        assertEquals("CM20 9TT", placeholders.get(EXELA_ADDRESS_POSTCODE_LITERAL));
    }

    @Test
    public void givenAScottishCaseAndPoBoxFeatureOff_thenSetExcelaAddressCorrectly() {
        ReflectionTestUtils.setField(placeholderService, "scottishPoBoxEnabled", false);

        Address address = Address.builder().line1("123 The Road").town("Brentwood").postcode("CM12 1TH").build();

        RegionalProcessingCenter rpc = RegionalProcessingCenter.builder()
            .name("Liverpool").address1(RPC_ADDRESS1).address2(RPC_ADDRESS2).address3(RPC_ADDRESS3)
            .address4(RPC_ADDRESS4).city(RPC_CITY).postcode(POSTCODE).phoneNumber(PHONE).build();

        Map<String, Object> placeholders = new HashMap<>();
        caseData = buildCaseData(rpc);
        caseData.setIsScottishCase("Yes");
        placeholderService.build(caseData, placeholders, address, now);

        assertEquals("HMCTS SSCS", placeholders.get(EXELA_ADDRESS_LINE1_LITERAL));
        assertEquals("PO BOX 12626", placeholders.get(EXELA_ADDRESS_LINE2_LITERAL));
        assertEquals("Harlow", placeholders.get(EXELA_ADDRESS_LINE3_LITERAL));
        assertEquals("CM20 9QF", placeholders.get(EXELA_ADDRESS_POSTCODE_LITERAL));
    }

    @Test
    public void givenACaseDataWithNoRpc_thenGenerateThePlaceholderMappingsWithoutRpc() {
        Address address = Address.builder().line1("123 The Road").town("Brentwood").postcode("CM12 1TH").build();
        caseData = buildCaseData(null);
        Map<String, Object> placeholders = new HashMap<>();

        placeholderService.build(caseData, placeholders, address, now);

        assertEquals(now, placeholders.get(CASE_CREATED_DATE_LITERAL));
        assertNull(placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE1_LITERAL));
        assertNull(placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE2_LITERAL));
        assertNull(placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE3_LITERAL));
        assertNull(placeholders.get(REGIONAL_OFFICE_ADDRESS_LINE4_LITERAL));
        assertNull(placeholders.get(REGIONAL_OFFICE_COUNTY_LITERAL));
        assertNull(placeholders.get(REGIONAL_OFFICE_POSTCODE_LITERAL));
    }

    @Test
    public void anAddressWithTwoLinesAndPostCodeWillNotHaveRow4() {
        Address address = Address.builder().line1("123 The Road").line2("Brentwood").postcode("CM12 1TH").build();

        caseData = buildCaseData(null);
        Map<String, Object> placeholders = new HashMap<>();

        placeholderService.build(caseData, placeholders, address, now);
        assertEquals(address.getLine1(), placeholders.get(RECIPIENT_ADDRESS_LINE_1_LITERAL));
        assertEquals(address.getLine2(), placeholders.get(RECIPIENT_ADDRESS_LINE_2_LITERAL));
        assertEquals(address.getPostcode(), placeholders.get(RECIPIENT_ADDRESS_LINE_3_LITERAL));
        assertNull(placeholders.get(RECIPIENT_ADDRESS_LINE_4_LITERAL));
    }

    private SscsCaseData buildCaseData(RegionalProcessingCenter rpc) {
        return SscsCaseData.builder()
            .ccdCaseId("123456")
            .regionalProcessingCenter(rpc)
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().dwpIssuingOffice("1").build())
                .benefitType(BenefitType.builder().code("PIP").description("Personal Independence Payment").build())
                .appellant(Appellant.builder()
                    .name(Name.builder().title("Mr").firstName("Terry").lastName("Tibbs").build())
                    .identity(Identity.builder().nino("JT0123456B").build())
                    .build())
                .build())
            .build();
    }
}
