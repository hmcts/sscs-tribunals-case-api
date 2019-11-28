package uk.gov.hmcts.reform.sscs.functional.ccd;

import static org.junit.Assert.*;
import static uk.gov.hmcts.reform.sscs.functional.ccd.UpdateCaseInCcdTest.buildSscsCaseDataForTestingWithValidMobileNumbers;
import static uk.gov.hmcts.reform.sscs.transform.deserialize.SubmitYourAppealToCcdCaseDataDeserializer.convertSyaToCcdCaseData;
import static uk.gov.hmcts.reform.sscs.util.SyaJsonMessageSerializer.*;
import static uk.gov.hmcts.reform.sscs.util.SyaServiceHelper.getRegionalProcessingCenter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
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

    @Test
    public void givenACaseWithDetails_thenCorrectlyUpdateAndDeserializeFromCcd() {
        SyaCaseWrapper syaCaseWrapper = ALL_DETAILS.getDeserializeMessage();

        SscsCaseData caseData = convertSyaToCcdCaseData(syaCaseWrapper);
        SscsCaseDetails caseDetails = ccdService.createCase(caseData, "appealCreated", "Appeal created summary", "Appeal created description", idamTokens);
        assertNotNull(caseDetails);

        caseData.setPanel(Panel.builder().assignedTo("Bill").disabilityQualifiedMember("Bob").medicalMember("Gary").build());

        EvidenceReceivedInformation evidenceReceivedInformation = new EvidenceReceivedInformation(new EvidenceReceivedInformationDetails("Yes", "2019-07-10"));
        List<EvidenceReceivedInformation> evidence = new ArrayList<>();
        evidence.add(evidenceReceivedInformation);
        caseData.setEvidenceReceived(EvidenceReceived.builder().appellantInfoRequestCollection(evidence).build());

        caseData.setUrgentCase("Yes");
        caseData.setDocumentSentToDwp("Yes");
        caseData.setDirectionDueDate("2019-10-10");
        caseData.setReservedToJudge("Judge Rinder");

        caseData.setIsWaiverNeeded("Yes");
        caseData.setWaiverDeclaration(Arrays.asList(new String[]{"waiverDeclarationText"}));
        caseData.setWaiverReason(Arrays.asList(new String[]{"nonCompliantOther", "nonCompliantNoMRN"}));
        caseData.setWaiverReasonOther("Not sure");
        caseData.setClerkDelegatedAuthority(Arrays.asList(new String[]{"delegatedAuthorityText"}));
        caseData.setClerkAppealSatisfactionText(Arrays.asList(new String[]{"appealSatisfactionText"}));
        caseData.setClerkConfirmationOfMrn("No");
        caseData.setClerkOtherReason("No");
        caseData.setClerkConfirmationOther("No idea");

        SscsCaseData updatedCaseData = ccdService.updateCase(caseData, caseDetails.getId(),
                "caseUpdated", "", "", idamTokens).getData();

        assertEquals("Bill", updatedCaseData.getPanel().getAssignedTo());
        assertEquals("Bob", updatedCaseData.getPanel().getDisabilityQualifiedMember());
        assertEquals("Gary", updatedCaseData.getPanel().getMedicalMember());
        assertEquals("Yes", updatedCaseData.getEvidenceReceived().getAppellantInfoRequestCollection().get(0).getValue().getEvidenceReceivedBoolean());
        assertEquals("2019-07-10", updatedCaseData.getEvidenceReceived().getAppellantInfoRequestCollection().get(0).getValue().getEvidenceReceivedDate());
        assertEquals("Yes", updatedCaseData.getUrgentCase());
        assertEquals("Yes", updatedCaseData.getDocumentSentToDwp());
        assertEquals("2019-10-10", updatedCaseData.getDirectionDueDate());
        assertEquals("Judge Rinder", updatedCaseData.getReservedToJudge());
        assertEquals("Yes", updatedCaseData.getIsWaiverNeeded());
        assertEquals("waiverDeclarationText", updatedCaseData.getWaiverDeclaration().get(0));
        assertEquals("nonCompliantOther", updatedCaseData.getWaiverReason().get(0));
        assertEquals("nonCompliantNoMRN", updatedCaseData.getWaiverReason().get(1));
        assertEquals("Not sure", updatedCaseData.getWaiverReasonOther());
        assertEquals("delegatedAuthorityText", updatedCaseData.getClerkDelegatedAuthority().get(0));
        assertEquals("appealSatisfactionText", updatedCaseData.getClerkAppealSatisfactionText().get(0));
        assertEquals("No", updatedCaseData.getClerkConfirmationOfMrn());
        assertEquals("No", updatedCaseData.getClerkOtherReason());
        assertEquals("No idea", updatedCaseData.getClerkConfirmationOther());
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
