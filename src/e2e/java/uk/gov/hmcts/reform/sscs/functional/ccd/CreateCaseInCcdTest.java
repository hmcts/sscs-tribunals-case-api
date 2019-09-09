package uk.gov.hmcts.reform.sscs.functional.ccd;

import static org.junit.Assert.*;
import static uk.gov.hmcts.reform.sscs.functional.ccd.UpdateCaseInCcdTest.buildSscsCaseDataForTestingWithValidMobileNumbers;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.*;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getRegionalProcessingCenter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@ContextConfiguration(initializers = CreateCaseInCcdTest.Initializer.class)
@SpringBootTest
public class CreateCaseInCcdTest {

    @Autowired
    private CcdService ccdService;

    @Autowired
    private IdamService idamService;

    private IdamTokens idamTokens;

    @Before
    public void setup() {
        idamTokens = idamService.getIdamTokens();
    }

    @Test
    public void givenACaseShouldBeSavedIntoCcd() {
        SscsCaseDetails caseDetails = ccdService.createCase(buildSscsCaseDataForTestingWithValidMobileNumbers(),
            "appealCreated", "Appeal created summary", "Appeal created description",
            idamTokens);
        assertNotNull(caseDetails);
    }

    @Test
    public void givenASyaCase_shouldBeSavedIntoCcdInCorrectState() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();
        RegionalProcessingCenter rpc = getRegionalProcessingCenter();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper, rpc.getName(), rpc);
        SscsCaseDetails caseDetails = ccdService.createCase(caseData, "appealCreated",
                "Appeal created summary", "Appeal created description", idamTokens);
        assertNotNull(caseDetails);
    }

    @Test
    public void givenASyaCaseWithoutAMatchingRpcShouldBeSavedIntoCcd() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();

        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper);
        SscsCaseDetails caseDetails = ccdService.createCase(caseData, "appealCreated", "Appeal created summary", "Appeal created description", idamTokens);
        assertNotNull(caseDetails);
    }

    @Test
    public void givenASyaCaseWithAppointeeDetailsWithSameAddressShouldBeSavedIntoCcd() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS.getDeserializeMessage();
        RegionalProcessingCenter rpc = getRegionalProcessingCenter();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper, rpc.getName(), rpc);
        SscsCaseDetails caseDetails = ccdService.createCase(caseData, "appealCreated", "Appeal created summary", "Appeal created description", idamTokens);
        assertNotNull(caseDetails);
        assertTrue(syaCaseWrapper.getAppellant().getIsAddressSameAsAppointee());
        assertEquals("Yes", caseData.getAppeal().getAppellant().getIsAddressSameAsAppointee());
    }

    @Test
    public void givenASyaCaseWithAppointeeDetailsWithDifferentAddressShouldBeSavedIntoCcd() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_DIFFERENT_ADDRESS.getDeserializeMessage();
        RegionalProcessingCenter rpc = getRegionalProcessingCenter();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper, rpc.getName(), rpc);
        SscsCaseDetails caseDetails = ccdService.createCase(caseData, "appealCreated", "Appeal created summary", "Appeal created description", idamTokens);
        assertNotNull(caseDetails);
        assertFalse(syaCaseWrapper.getAppellant().getIsAddressSameAsAppointee());
        assertEquals("No", caseData.getAppeal().getAppellant().getIsAddressSameAsAppointee());
    }

    @Test
    public void givenASyaCaseWithAppointeeDetailsWithSameAddressButNoAppellantContactDetailsShouldBeSavedIntoCcd() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS_WITH_APPOINTEE_AND_SAME_ADDRESS_BUT_NO_APPELLANT_CONTACT_DETAILS.getDeserializeMessage();
        RegionalProcessingCenter rpc = getRegionalProcessingCenter();
        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper,
            rpc.getName(), rpc);
        SscsCaseDetails caseDetails = ccdService.createCase(caseData, "appealCreated", "Appeal created summary", "Appeal created description", idamTokens);
        assertNotNull(caseDetails);
        assertTrue(syaCaseWrapper.getAppellant().getIsAddressSameAsAppointee());
        assertEquals("Yes", caseData.getAppeal().getAppellant().getIsAddressSameAsAppointee());
    }

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        private final String env = System.getenv("ENV");

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            if (env != null && env.equals("LOCAL")) {
                TestPropertyValues.of("pdf.api.url=http://localhost:5500");
            }
        }
    }
}
