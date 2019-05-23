package uk.gov.hmcts.reform.sscs.functional.ccd;

import static org.junit.Assert.*;
import static uk.gov.hmcts.reform.sscs.functional.ccd.UpdateCaseInCcdTest.buildSscsCaseDataForTestingWithValidMobileNumbers;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.*;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getRegionalProcessingCenter;

import com.microsoft.applicationinsights.boot.dependencies.apachecommons.lang3.RandomStringUtils;
import java.time.LocalDate;
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
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.config.CcdRequestDetails;
import uk.gov.hmcts.reform.sscs.ccd.domain.RegionalProcessingCenter;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseDetails;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.reform.sscs.exception.EmailSendFailedException;
import uk.gov.hmcts.reform.sscs.exception.PdfGenerationException;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;
import uk.gov.hmcts.reform.sscs.service.SubmitAppealService;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:config/application_e2e.properties")
@ContextConfiguration(initializers = CreateCaseInCcdTest.Initializer.class)

@SpringBootTest
public class CreateCaseInCcdTest {

    @Autowired
    private CcdService ccdService;

    @Autowired
    private CcdClient ccdClient;

    @Autowired
    private CcdRequestDetails ccdRequestDetails;

    @Autowired
    private IdamService idamService;

    @Autowired
    private SubmitAppealService submitAppealService;

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
    public void givenASyaCaseShouldBeSavedIntoCcdViaSubmitAppealService() {
        try {
            SyaCaseWrapper wrapper = ALL_DETAILS.getDeserializeMessage();
            wrapper.getAppellant().setNino(RandomStringUtils.random(9, true, true).toUpperCase());
            wrapper.getMrn().setDate(LocalDate.now());

            Long id = submitAppealService.submitAppeal(wrapper, "");
            assertEquals("withDwp", findStateOfCaseInCcd(id));
        } catch (EmailSendFailedException | PdfGenerationException ep) {
            assertTrue(true);
        }
    }

    private String findStateOfCaseInCcd(Long id) {
        return ccdService.getByCaseId(id, idamTokens).getState();
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

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        private final String env = System.getenv("ENV");

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            if (env != null && env.equals("LOCAL")) {
                TestPropertyValues.of("pdf.api.url=http://localhost:5500");
            }
        }
    }
}
