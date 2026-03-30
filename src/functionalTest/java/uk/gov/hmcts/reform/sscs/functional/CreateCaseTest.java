package uk.gov.hmcts.reform.sscs.functional;

import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_BENEFIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.CHILD_SUPPORT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.GUARANTEED_MINIMUM_PENSION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.GUARDIANS_ALLOWANCE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.HOME_RESPONSIBILITIES_PROTECTION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.INFECTED_BLOOD_COMPENSATION;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.NATIONAL_INSURANCE_CREDITS;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.TAX_CREDIT;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.TAX_FREE_CHILDCARE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.THIRTY_HOURS_FREE_CHILDCARE;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.UC;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.INCOMPLETE_APPLICATION_RECEIVED;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.VALID_APPEAL;

import io.restassured.RestAssured;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.sscs.ccd.domain.Address;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appeal;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appellant;
import uk.gov.hmcts.reform.sscs.ccd.domain.Appointee;
import uk.gov.hmcts.reform.sscs.ccd.domain.Benefit;
import uk.gov.hmcts.reform.sscs.ccd.domain.BenefitType;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseAccessManagementFields;
import uk.gov.hmcts.reform.sscs.ccd.domain.CaseManagementLocation;
import uk.gov.hmcts.reform.sscs.ccd.domain.Contact;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicList;
import uk.gov.hmcts.reform.sscs.ccd.domain.DynamicListItem;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingOptions;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.Identity;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Name;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.Representative;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscription;
import uk.gov.hmcts.reform.sscs.ccd.domain.Subscriptions;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @Autowired)
//@Disabled
class CreateCaseTest {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Map<Benefit, String> DW_ISSUING_OFFICE;

    static {
        DW_ISSUING_OFFICE = new java.util.HashMap<>();
        DW_ISSUING_OFFICE.put(Benefit.ESA, "Balham DRT");
        DW_ISSUING_OFFICE.put(CHILD_SUPPORT, "Child Support");
        DW_ISSUING_OFFICE.put(INFECTED_BLOOD_COMPENSATION, "Balham DRT");
        DW_ISSUING_OFFICE.put(TAX_CREDIT, "Balham DRT");
        DW_ISSUING_OFFICE.put(GUARDIANS_ALLOWANCE, "Balham DRT");
        DW_ISSUING_OFFICE.put(TAX_FREE_CHILDCARE, "Balham DRT");
        DW_ISSUING_OFFICE.put(HOME_RESPONSIBILITIES_PROTECTION, "Balham DRT");
        DW_ISSUING_OFFICE.put(CHILD_BENEFIT, "Balham DRT");
        DW_ISSUING_OFFICE.put(THIRTY_HOURS_FREE_CHILDCARE, "Balham DRT");
        DW_ISSUING_OFFICE.put(GUARANTEED_MINIMUM_PENSION, "Balham DRT");
        DW_ISSUING_OFFICE.put(NATIONAL_INSURANCE_CREDITS, "Balham DRT");
        DW_ISSUING_OFFICE.put(UC, "Balham DRT");
    }

    private final CcdService ccdService;
    private final IdamService idamService;
    private IdamTokens idamTokens;

    static String generateNino() {
        return RandomStringUtils.secure().next(9, true, true).toUpperCase();
    }

    @BeforeEach
    void setup() {
        idamTokens = idamService.getIdamTokens();
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void shouldCreateATaxCreditCase() {
        SscsCaseData sscsCaseData = build(TAX_CREDIT);
        final SscsCaseDetails caseDetails = createCase(sscsCaseData, INCOMPLETE_APPLICATION_RECEIVED.getCcdType());
        ccdService.updateCase(sscsCaseData, caseDetails.getId(), VALID_APPEAL.getCcdType(), "Update Case from functional test",
            "Test case", idamTokens);
        updateCasesFile(TAX_CREDIT.getShortName(), caseDetails.getId());
    }

    @Test
    void shouldCreateAGuardiansAllowanceCase() {
        SscsCaseData sscsCaseData = build(GUARDIANS_ALLOWANCE);
        final SscsCaseDetails caseDetails = createCase(sscsCaseData, INCOMPLETE_APPLICATION_RECEIVED.getCcdType());
        ccdService.updateCase(sscsCaseData, caseDetails.getId(), VALID_APPEAL.getCcdType(), "Update Case from functional test",
            "Test case", idamTokens);
        updateCasesFile(GUARDIANS_ALLOWANCE.getShortName(), caseDetails.getId());
    }

    @Test
    void shouldCreateATaxFreeChildCareCase() {
        SscsCaseData sscsCaseData = build(TAX_FREE_CHILDCARE);
        final SscsCaseDetails caseDetails = createCase(sscsCaseData, INCOMPLETE_APPLICATION_RECEIVED.getCcdType());
        ccdService.updateCase(sscsCaseData, caseDetails.getId(), VALID_APPEAL.getCcdType(), "Update Case from functional test",
            "Test case", idamTokens);
        updateCasesFile(TAX_FREE_CHILDCARE.getShortName(), caseDetails.getId());
    }

    @Test
    void shouldCreateAHomeResponsibilitiesProtectionCase() {
        SscsCaseData sscsCaseData = build(HOME_RESPONSIBILITIES_PROTECTION);
        final SscsCaseDetails caseDetails = createCase(sscsCaseData, INCOMPLETE_APPLICATION_RECEIVED.getCcdType());
        ccdService.updateCase(sscsCaseData, caseDetails.getId(), VALID_APPEAL.getCcdType(), "Update Case from functional test",
            "Test case", idamTokens);
        updateCasesFile(HOME_RESPONSIBILITIES_PROTECTION.getShortName(), caseDetails.getId());
    }

    @Test
    void shouldCreateAChildBenefitCase() {
        SscsCaseData sscsCaseData = build(CHILD_BENEFIT);
        final SscsCaseDetails caseDetails = createCase(sscsCaseData, INCOMPLETE_APPLICATION_RECEIVED.getCcdType());
        ccdService.updateCase(sscsCaseData, caseDetails.getId(), VALID_APPEAL.getCcdType(), "Update Case from functional test",
            "Test case", idamTokens);
        updateCasesFile(CHILD_BENEFIT.getShortName(), caseDetails.getId());
    }

    @Test
    void shouldCreateAThirtyHoursFreeChildcareCase() {
        SscsCaseData sscsCaseData = build(THIRTY_HOURS_FREE_CHILDCARE);
        final SscsCaseDetails caseDetails = createCase(sscsCaseData, INCOMPLETE_APPLICATION_RECEIVED.getCcdType());
        ccdService.updateCase(sscsCaseData, caseDetails.getId(), VALID_APPEAL.getCcdType(), "Update Case from functional test",
            "Test case", idamTokens);
        updateCasesFile(THIRTY_HOURS_FREE_CHILDCARE.getShortName(), caseDetails.getId());
    }

    @Test
    void shouldCreateAGuaranteedMinimumPensionCase() {
        SscsCaseData sscsCaseData = build(GUARANTEED_MINIMUM_PENSION);
        final SscsCaseDetails caseDetails = createCase(sscsCaseData, INCOMPLETE_APPLICATION_RECEIVED.getCcdType());
        ccdService.updateCase(sscsCaseData, caseDetails.getId(), VALID_APPEAL.getCcdType(), "Update Case from functional test",
            "Test case", idamTokens);
        updateCasesFile(GUARANTEED_MINIMUM_PENSION.getShortName(), caseDetails.getId());
    }

    @Test
    void shouldCreateANationalInsuranceCreditsCase() {
        SscsCaseData sscsCaseData = build(NATIONAL_INSURANCE_CREDITS);
        final SscsCaseDetails caseDetails = createCase(sscsCaseData, INCOMPLETE_APPLICATION_RECEIVED.getCcdType());
        ccdService.updateCase(sscsCaseData, caseDetails.getId(), VALID_APPEAL.getCcdType(), "Update Case from functional test",
            "Test case", idamTokens);
        updateCasesFile(NATIONAL_INSURANCE_CREDITS.getShortName(), caseDetails.getId());
    }

    @Test
    void shouldCreateAInfectedBloodCase() {
        SscsCaseData sscsCaseData = buildInfectedBlood(INFECTED_BLOOD_COMPENSATION);
        final SscsCaseDetails caseDetails = createCase(sscsCaseData, INCOMPLETE_APPLICATION_RECEIVED.getCcdType());
        ccdService.updateCase(sscsCaseData, caseDetails.getId(), VALID_APPEAL.getCcdType(), "Update Case from functional test",
            "Test case", idamTokens);
        updateCasesFile(INFECTED_BLOOD_COMPENSATION.getShortName(), caseDetails.getId());
    }

    @Test
    void shouldCreateAUniversalCreditCase() {
        SscsCaseData sscsCaseData = build(UC);
        final SscsCaseDetails caseDetails = createCase(sscsCaseData, INCOMPLETE_APPLICATION_RECEIVED.getCcdType());
        ccdService.updateCase(sscsCaseData, caseDetails.getId(), VALID_APPEAL.getCcdType(), "Update Case from functional test",
            "Test case", idamTokens);
        updateCasesFile(UC.getShortName(), caseDetails.getId());
    }

    @Test
    void shouldCreateAChildSupportCase() {
        SscsCaseData sscsCaseData = build(CHILD_SUPPORT);
        final SscsCaseDetails caseDetails = createCase(sscsCaseData, INCOMPLETE_APPLICATION_RECEIVED.getCcdType());
        ccdService.updateCase(sscsCaseData, caseDetails.getId(), VALID_APPEAL.getCcdType(), "Update Case from functional test",
            "Test case", idamTokens);
        updateCasesFile(CHILD_SUPPORT.getShortName(), caseDetails.getId());
    }

    @Test
    void shouldCreateAnEsaCase() {
        SscsCaseData sscsCaseData = build(Benefit.ESA);
        final SscsCaseDetails caseDetails = createCase(sscsCaseData, INCOMPLETE_APPLICATION_RECEIVED.getCcdType());
        ccdService.updateCase(sscsCaseData, caseDetails.getId(), VALID_APPEAL.getCcdType(), "Update Case from functional test",
            "Test case", idamTokens);
        updateCasesFile(Benefit.ESA.getShortName(), caseDetails.getId());
    }

    private SscsCaseData buildInfectedBlood(Benefit benefitCode) {
        return SscsCaseData.builder().caseCreated("2025-12-02").benefitCode(benefitCode.getBenefitCode()).issueCode("DD")
            .caseCode("022DD").region("BRADFORD")
            .caseManagementLocation(CaseManagementLocation.builder().baseLocation("698118").region("6").build())
            .regionalProcessingCenter(
                RegionalProcessingCenter.builder().name("BRADFORD").address1("HM Courts & Tribunals Service")
                    .address2("Social Security & Child Support Appeals").address3("Phoenix House").address4("Rushton Avenue")
                    .postcode("BD3 7BH").phoneNumber("07398785051").faxNumber("07398785051").email("SSCS_Bradford@justice.gov.uk")
                    .build()).subscriptions(Subscriptions.builder().appointeeSubscription(
                    Subscription.builder().wantSmsNotifications("Yes").email("sscstest+notify@greencroftconsulting.com")
                        .mobile("07398785051").subscribeSms("Yes").subscribeEmail("Yes").tya(generateRandomString()).build())
                .appellantSubscription(
                    Subscription.builder().wantSmsNotifications("Yes").email("sscstest+notify@greencroftconsulting.com")
                        .mobile("07398785051").subscribeSms("Yes").subscribeEmail("Yes").tya(generateRandomString()).build())
                .representativeSubscription(
                    Subscription.builder().wantSmsNotifications("Yes").email("sscstest+notify@greencroftconsulting.com")
                        .mobile("07398785051").subscribeEmail("Yes").subscribeSms("Yes").tya(generateRandomString()).build())
                .build()).caseAccessManagementFields(CaseAccessManagementFields.builder().caseAccessCategory("childASupport")
                .caseManagementCategory(
                    new DynamicList(new DynamicListItem(benefitCode.getShortName(), benefitCode.getDescription()),
                        List.of(new DynamicListItem(benefitCode.getShortName(), benefitCode.getDescription())))).build()).appeal(
                Appeal.builder().rep(Representative.builder().hasRepresentative("Yes").address(
                            Address.builder().line1("Buckingham Palace").town("London").county("Greater London").country("United Kingdom")
                                .postcode("TS1 1ST").build()).organisation("Widgets R Us")
                        .identity(Identity.builder().dob("1983-02-11").nino(generateNino()).build())
                        .contact(Contact.builder().email("sscstest+notify@greencroftconsulting.com").mobile("07398785051").build())
                        .build()).benefitType(
                        BenefitType.builder().code(benefitCode.getShortName()).description(benefitCode.getDescription()).build())
                    .appellant(Appellant.builder().appointee(
                            Appointee.builder().name(Name.builder().title("Mrs").firstName("Mary").lastName("Bloggs").build())
                                .address(Address.builder().line1("Buckingham Palace").town("London").county("Greater London")
                                    .country("United Kingdom").postcode("TS1 1ST").build()).contact(
                                    Contact.builder().email("sscsappointee@greencroftconsulting.com").phone("07398785051").build())
                                .identity(Identity.builder().dob("1991-03-31").nino(generateNino()).build()).build())
                        .name(Name.builder().title("Mr").firstName("John").lastName("Hewitt").build()).identity(
                            Identity.builder().dob("1983-02-11")
                                .nino("AB%sC".formatted(ThreadLocalRandom.current().nextInt(100000, 1_000_000)))
                                .ibcaReference("A11B22").build()).address(
                            Address.builder().line1("Buckingham Palace").town("London").county("Greater London")
                                .country("United Kingdom").postcode("TS1 1ST").build())
                        .contact(Contact.builder().email("sscsappointee@greencroftconsulting.com").mobile("07398785051").build())
                        .build()).hearingOptions(HearingOptions.builder().hearingRoute(HearingRoute.GAPS).build()).mrnDetails(
                        MrnDetails.builder().mrnDate("2024-12-18").dwpIssuingOffice(DW_ISSUING_OFFICE.get(benefitCode)).build())
                    .hearingType("oral").build()).build();
    }

    private SscsCaseData build(Benefit benefitCode) {
        return SscsCaseData.builder().caseCreated("2025-12-02").benefitCode(benefitCode.getBenefitCode()).issueCode("DD")
            .caseCode("022DD").region("BRADFORD")
            .caseManagementLocation(CaseManagementLocation.builder().baseLocation("698118").region("6").build())
            .regionalProcessingCenter(
                RegionalProcessingCenter.builder().name("BRADFORD").address1("HM Courts & Tribunals Service")
                    .address2("Social Security & Child Support Appeals").address3("Phoenix House").address4("Rushton Avenue")
                    .postcode("BD3 7BH").phoneNumber("07398785051").faxNumber("07398785051").email("SSCS_Bradford@justice.gov.uk")
                    .build()).subscriptions(Subscriptions.builder().appointeeSubscription(
                    Subscription.builder().wantSmsNotifications("Yes").email("sscstest+notify@greencroftconsulting.com")
                        .mobile("07398785051").subscribeSms("Yes").subscribeEmail("Yes").tya(generateRandomString()).build())
                .appellantSubscription(
                    Subscription.builder().wantSmsNotifications("Yes").email("sscstest+notify@greencroftconsulting.com")
                        .mobile("07398785051").subscribeSms("Yes").subscribeEmail("Yes").tya(generateRandomString()).build())
                .representativeSubscription(
                    Subscription.builder().wantSmsNotifications("Yes").email("sscstest+notify@greencroftconsulting.com")
                        .mobile("07398785051").subscribeEmail("Yes").subscribeSms("Yes").tya(generateRandomString()).build())
                .build()).caseAccessManagementFields(CaseAccessManagementFields.builder().caseAccessCategory("childASupport")
                .caseManagementCategory(
                    new DynamicList(new DynamicListItem(benefitCode.getShortName(), benefitCode.getDescription()),
                        List.of(new DynamicListItem(benefitCode.getShortName(), benefitCode.getDescription())))).build()).appeal(
                Appeal.builder().rep(Representative.builder().hasRepresentative("Yes").address(
                            Address.builder().line1("Buckingham Palace").town("London").county("Greater London").country("United Kingdom")
                                .postcode("TS1 1ST").build()).organisation("Widgets R Us")
                        .identity(Identity.builder().dob("1983-02-11").nino(generateNino()).build())
                        .contact(Contact.builder().email("sscstest+notify@greencroftconsulting.com").mobile("07398785051").build())
                        .build()).benefitType(
                        BenefitType.builder().code(benefitCode.getShortName()).description(benefitCode.getDescription()).build())
                    .appellant(Appellant.builder().appointee(
                            Appointee.builder().name(Name.builder().title("Mrs").firstName("Mary").lastName("Bloggs").build())
                                .address(Address.builder().line1("Buckingham Palace").town("London").county("Greater London")
                                    .country("United Kingdom").postcode("TS1 1ST").build()).contact(
                                    Contact.builder().email("sscsappointee@greencroftconsulting.com").phone("07398785051").build())
                                .identity(Identity.builder().dob("1991-03-31").nino(generateNino()).build()).build())
                        .name(Name.builder().title("Mr").firstName("John").lastName("Hewitt").build()).identity(
                            Identity.builder().dob("1983-02-11")
                                .nino("AB%sC".formatted(ThreadLocalRandom.current().nextInt(100000, 1_000_000))).build()).address(
                            Address.builder().line1("Buckingham Palace").town("London").county("Greater London")
                                .country("United Kingdom").postcode("TS1 1ST").build())
                        .contact(Contact.builder().email("sscsappointee@greencroftconsulting.com").mobile("07398785051").build())
                        .build()).hearingOptions(HearingOptions.builder().hearingRoute(HearingRoute.LIST_ASSIST).build())
                    .mrnDetails(
                        MrnDetails.builder().mrnDate("2024-12-18").dwpIssuingOffice(DW_ISSUING_OFFICE.get(benefitCode)).build())
                    .hearingType("oral").build()).build();
    }

    private SscsCaseData buildTaxCredit(Benefit benefitCode) {
        return SscsCaseData.builder().caseCreated("2025-12-02").benefitCode(benefitCode.getBenefitCode()).issueCode("DD")
            .caseCode("022DD").region("BRADFORD")
            .caseManagementLocation(CaseManagementLocation.builder().baseLocation("698118").region("6").build())
            .regionalProcessingCenter(
                RegionalProcessingCenter.builder().name("BRADFORD").address1("HM Courts & Tribunals Service")
                    .address2("Social Security & Child Support Appeals").address3("Phoenix House").address4("Rushton Avenue")
                    .postcode("BD3 7BH").phoneNumber("07398785051").faxNumber("07398785051").email("SSCS_Bradford@justice.gov.uk")
                    .build()).subscriptions(Subscriptions.builder().appointeeSubscription(
                    Subscription.builder().wantSmsNotifications("Yes").email("sscstest+notify@greencroftconsulting.com")
                        .mobile("07398785051").subscribeSms("Yes").subscribeEmail("Yes").tya(generateRandomString()).build())
                .appellantSubscription(
                    Subscription.builder().wantSmsNotifications("Yes").email("sscstest+notify@greencroftconsulting.com")
                        .mobile("07398785051").subscribeSms("Yes").subscribeEmail("Yes").tya(generateRandomString()).build())
                .representativeSubscription(
                    Subscription.builder().wantSmsNotifications("Yes").email("sscstest+notify@greencroftconsulting.com")
                        .mobile("07398785051").subscribeEmail("Yes").subscribeSms("Yes").tya(generateRandomString()).build())
                .build()).caseAccessManagementFields(CaseAccessManagementFields.builder().caseAccessCategory("childASupport")
                .caseManagementCategory(
                    new DynamicList(new DynamicListItem(benefitCode.getShortName(), benefitCode.getDescription()),
                        List.of(new DynamicListItem(benefitCode.getShortName(), benefitCode.getDescription())))).build()).appeal(
                Appeal.builder().mrnDetails(MrnDetails.builder().dwpIssuingOffice(DW_ISSUING_OFFICE.get(benefitCode)).build())
                    .rep(Representative.builder().hasRepresentative("Yes").address(
                            Address.builder().line1("Buckingham Palace").town("London").county("Greater London")
                                .country("United Kingdom").postcode("TS1 1ST").build()).organisation("Widgets R Us")
                        .identity(Identity.builder().dob("1983-02-11").nino(generateNino()).build()).contact(
                            Contact.builder().email("sscstest+notify@greencroftconsulting.com").mobile("07398785051").build())
                        .build()).benefitType(
                        BenefitType.builder().code(benefitCode.getShortName()).description(benefitCode.getDescription()).build())
                    .appellant(Appellant.builder().appointee(
                            Appointee.builder().name(Name.builder().title("Mrs").firstName("Mary").lastName("Bloggs").build())
                                .address(Address.builder().line1("Buckingham Palace").town("London").county("Greater London")
                                    .country("United Kingdom").postcode("TS1 1ST").build()).contact(
                                    Contact.builder().email("sscsappointee@greencroftconsulting.com").phone("07398785051").build())
                                .identity(Identity.builder().dob("1991-03-31").nino(generateNino()).build()).build())
                        .name(Name.builder().title("Mr").firstName("John").lastName("Hewitt").build()).identity(
                            Identity.builder().dob("1983-02-11")
                                .nino("AB%sC".formatted(ThreadLocalRandom.current().nextInt(100000, 1_000_000))).build()).address(
                            Address.builder().line1("Buckingham Palace").town("London").county("Greater London")
                                .country("United Kingdom").postcode("TS1 1ST").build())
                        .contact(Contact.builder().email("sscsappointee@greencroftconsulting.com").mobile("07398785051").build())
                        .build()).hearingOptions(HearingOptions.builder().hearingRoute(HearingRoute.GAPS).build()).mrnDetails(
                        MrnDetails.builder().mrnDate("2024-12-18").dwpIssuingOffice(DW_ISSUING_OFFICE.get(benefitCode)).build())
                    .hearingType("oral").build()).build();
    }

    private SscsCaseDetails createCase(SscsCaseData caseData, String ccdType) {
        return ccdService.createCase(caseData, ccdType, "SSCS: Creating a Test Case from FT", "Test Case", idamTokens);
    }

    private String generateRandomString() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();

    }

    private void updateCasesFile(String benefitKey, Long caseId) {
        try {
            Path outputDir = Paths.get("build/output");
            Files.createDirectories(outputDir);
            Path casesFile = outputDir.resolve("cases.txt");

            Properties properties = new Properties();
            if (Files.exists(casesFile)) {
                try (var reader = Files.newBufferedReader(casesFile)) {
                    properties.load(reader);
                }
            }

            properties.setProperty(benefitKey, String.valueOf(caseId));

            try (var writer = Files.newBufferedWriter(casesFile, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
                properties.store(writer, "Case Details");
            }
        } catch (IOException e) {
            log.error("Failed to update cases.txt file", e);
        }
    }
}