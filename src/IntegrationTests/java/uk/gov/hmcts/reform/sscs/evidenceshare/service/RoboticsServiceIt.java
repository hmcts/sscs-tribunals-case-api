package uk.gov.hmcts.reform.sscs.evidenceshare.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.sscs.ccd.domain.State.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.hmcts.reform.sscs.ccd.client.CcdClient;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.ccd.service.CcdService;
import uk.gov.hmcts.reform.sscs.idam.IdamService;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsJsonMapper;
import uk.gov.hmcts.reform.sscs.robotics.RoboticsWrapper;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations = "classpath:config/application_es_it.properties")
public class RoboticsServiceIt {

    @Autowired
    private RoboticsService roboticsService;

    @MockBean
    private IdamService idamService;

    @MockBean
    private CcdClient ccdClient;

    @MockBean
    private CcdService ccdService;

    @Autowired
    private RoboticsJsonMapper mapper;

    private SscsCaseData caseData;

    private RoboticsWrapper roboticsWrapper;

    private CaseDetails<SscsCaseData> caseDetails;

    @MockBean
    private EmailService emailService;

    @Captor
    private ArgumentCaptor<SscsCaseData> caseDataCaptor;

    @Mock
    private SscsCaseDetails sscsCaseDetails;


    @Before
    public void setup() {
        caseData = SscsCaseData.builder()
            .ccdCaseId("123456")
            .regionalProcessingCenter(null)
            .evidencePresent("Yes")
            .appeal(Appeal.builder()
                .mrnDetails(MrnDetails.builder().dwpIssuingOffice("DWP PIP (1)").build())
                .benefitType(BenefitType.builder().code("PIP").description("Personal Independence Payment").build())
                .receivedVia("paper")
                .appellant(Appellant.builder()
                    .name(Name.builder().title("Mr").firstName("Terry").lastName("Tibbs").build())
                    .address(Address.builder().line1("99 My Road").town("Grantham").county("Surrey").postcode("CV10 6PO").build())
                    .identity(Identity.builder().nino("JT0123456B").build())
                    .contact(Contact.builder().mobile(null).email(null).build())
                    .build())
                .hearingOptions(HearingOptions.builder().wantsToAttend("Yes").other("My hearing").build())
                .build())
            .build();

        caseDetails = new CaseDetails<>(1234L, "sscs", APPEAL_CREATED, caseData, null, "Benefit");


        when(ccdService.getByCaseId(any(), any())).thenReturn(sscsCaseDetails);
        when(sscsCaseDetails.getData()).thenReturn(caseData);
    }

    @Test
    public void givenSscsCaseDataWithRep_makeValidRoboticsJsonThatValidatesAgainstSchema() {

        caseData.getAppeal().setRep(Representative.builder()
            .hasRepresentative("Yes").name(Name.builder().title("Mrs").firstName("Wendy").lastName("Barker").build())
            .address(Address.builder().line1("99 My Road").town("Grantham").county("Surrey").postcode("RH5 6PO").build())
            .contact(Contact.builder().mobile(null).email(null).build())
            .build());

        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertTrue(result.has("representative"));
        assertTrue(result.has("hearingArrangements"));

        verifyNoMoreInteractions(ccdService);
    }

    @Test
    public void givenSscsCaseDataWithoutRepresentative_makeValidRoboticsJsonThatValidatesAgainstSchema() {
        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertFalse(result.has("representative"));
        assertTrue(result.has("hearingArrangements"));
        assertTrue(result.has("isReadyToList"));

        verifyNoMoreInteractions(ccdService);
    }

    @Test
    public void givenSscsCaseDataWithUcBenefitType_makeValidRoboticsJsonThatValidatesAgainstSchema() {
        caseDetails.getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("UC").build());
        caseDetails.getCaseData().getAppeal().setMrnDetails(MrnDetails.builder().dwpIssuingOffice("Universal Credit").build());

        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertFalse(result.has("representative"));
        assertTrue(result.has("hearingArrangements"));
        assertTrue(result.has("isReadyToList"));
        assertEquals("Coventry (CMCB)", result.get("appellantPostCode"));

        verifyNoMoreInteractions(ccdService);
    }

    @Test
    public void givenSscsCaseDataWithEsaBenefitType_makeValidRoboticsJsonThatValidatesAgainstSchema() {
        caseDetails.getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("ESA").build());
        caseDetails.getCaseData().getAppeal().setMrnDetails(MrnDetails.builder().dwpIssuingOffice("Birkenhead LM DRT").build());

        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertFalse(result.has("representative"));
        assertTrue(result.has("hearingArrangements"));
        assertTrue(result.has("isReadyToList"));
        assertEquals("Coventry (CMCB)", result.get("appellantPostCode"));

        verifyNoMoreInteractions(ccdService);
    }

    @Test
    public void givenSscsCaseDataWithPipBenefitType_makeValidRoboticsJsonThatValidatesAgainstSchema() {
        caseDetails.getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("Pip").build());

        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertFalse(result.has("representative"));
        assertTrue(result.has("hearingArrangements"));
        assertTrue(result.has("isReadyToList"));
        assertEquals("Nuneaton", result.get("appellantPostCode"));

        verifyNoMoreInteractions(ccdService);
    }

    @Test
    public void givenSscsCaseDataWithAppointeeAndPipBenefitType_makeValidRoboticsJsonThatValidatesAgainstSchemaWithAppointeePostcode() {
        caseDetails.getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("Pip").build());

        caseDetails.getCaseData().getAppeal().setAppellant(Appellant.builder()
            .name(Name.builder().title("Mr").firstName("Terry").lastName("Tibbs").build())
            .address(Address.builder().line1("99 My Road").town("Grantham").county("Surrey").postcode("CV10 6PO").build())
            .identity(Identity.builder().nino("JT0123456B").build())
            .contact(Contact.builder().mobile(null).email(null).build())
            .isAppointee("Yes").appointee(
                Appointee.builder().name(Name.builder().title("Mr").firstName("Terry").lastName("Tibbs").build())
                    .identity(Identity.builder().nino("JT0123456B").build())
                    .address(Address.builder().line1("99 My Road").town("Chelmsford").county("Essex").postcode("CM12 0NS").build())
                    .contact(Contact.builder().mobile(null).email(null).build())
                    .build()).build());

        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertFalse(result.has("representative"));
        assertTrue(result.has("hearingArrangements"));
        assertTrue(result.has("isReadyToList"));
        assertEquals("Basildon CC", result.get("appellantPostCode"));

        verifyNoMoreInteractions(ccdService);
    }

    @Test
    public void givenSscsCaseDataWithoutHearingArrangements_makeValidRoboticsJsonThatValidatesAgainstSchema() {
        caseData.getAppeal().setHearingOptions(HearingOptions.builder().wantsToAttend("Yes").build());

        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertFalse(result.has("representative"));
        assertFalse(result.has("hearingArrangements"));

        verifyNoMoreInteractions(ccdService);
    }

    @Test
    public void givenUcSscsCaseDataWithJointPartyAndElements_makeValidRoboticsJsonThatValidatesAgainstSchema() {
        caseDetails.getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("Pip").build());
        caseDetails.getCaseData().setElementsDisputedLinkedAppealRef("123456");
        caseDetails.getCaseData().setElementsDisputedIsDecisionDisputedByOthers("Yes");

        caseDetails.getCaseData().getJointParty().setHasJointParty(YES);
        caseDetails.getCaseData().getJointParty().setName(Name.builder().title("Mr").firstName("Terry").lastName("Tibbs").build());
        caseDetails.getCaseData().getJointParty().setIdentity(Identity.builder().nino("JT0123456B").dob("2000-01-01").build());
        caseDetails.getCaseData().getJointParty().setAddress(Address.builder().line1("99 My Road").town("Chelmsford").county("Essex").postcode("CM12 0NS").build());

        List<ElementDisputed> elementsDisputedGeneralList = new ArrayList<>();
        elementsDisputedGeneralList.add(ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("firstIssueElementsDisputedGeneral1").build()).build());
        elementsDisputedGeneralList.add(ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("secondIssueElementsDisputedGeneral").build()).build());

        List<ElementDisputed> elementsDisputedSanctionsList = new ArrayList<>();
        elementsDisputedSanctionsList.add(ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("firstIssueElementsDisputedSanctions").build()).build());

        List<ElementDisputed> elementsDisputedOverpaymentList = new ArrayList<>();
        elementsDisputedOverpaymentList.add(ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("firstIssueElementsDisputedOverpayment").build()).build());

        List<ElementDisputed> elementsDisputedHousingList = new ArrayList<>();
        elementsDisputedHousingList.add(ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("firstIssueElementsDisputedHousing").build()).build());

        List<ElementDisputed> elementsDisputedChildCareList = new ArrayList<>();
        elementsDisputedChildCareList.add(ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("firstIssueElementsDisputedChildCare").build()).build());

        List<ElementDisputed> elementsDisputedCareList = new ArrayList<>();
        elementsDisputedCareList.add(ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("firstIssueElementsDisputedCare").build()).build());

        List<ElementDisputed> elementsDisputedChildElementList = new ArrayList<>();
        elementsDisputedChildElementList.add(ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("firstIssueElementsDisputedChildElement").build()).build());

        List<ElementDisputed> elementsDisputedChildDisabledList = new ArrayList<>();
        elementsDisputedChildDisabledList.add(ElementDisputed.builder().value(ElementDisputedDetails.builder().issueCode("firstIssueElementsDisputedChildDisabled").build()).build());

        caseDetails.getCaseData().setElementsDisputedGeneral(elementsDisputedGeneralList);
        caseDetails.getCaseData().setElementsDisputedSanctions(elementsDisputedSanctionsList);
        caseDetails.getCaseData().setElementsDisputedOverpayment(elementsDisputedOverpaymentList);
        caseDetails.getCaseData().setElementsDisputedHousing(elementsDisputedHousingList);
        caseDetails.getCaseData().setElementsDisputedChildCare(elementsDisputedChildCareList);
        caseDetails.getCaseData().setElementsDisputedCare(elementsDisputedCareList);
        caseDetails.getCaseData().setElementsDisputedChildElement(elementsDisputedChildElementList);
        caseDetails.getCaseData().setElementsDisputedChildDisabled(elementsDisputedChildDisabledList);

        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("jointParty"));
        assertTrue(result.has("elementsDisputed"));
        assertTrue(result.has("linkedAppealRef"));
        assertTrue(result.has("ucDecisionDisputedByOthers"));

        verifyNoMoreInteractions(ccdService);
    }

    @Test
    public void givenReviewConfidentialityRequest_thenAddIsConfidentialFlagToRobotics() {
        caseDetails = new CaseDetails<>(1234L, "sscs", RESPONSE_RECEIVED, caseData, null, "Benefit");

        caseDetails.getCaseData().setConfidentialityRequestOutcomeAppellant(DatedRequestOutcome.builder().requestOutcome(RequestOutcome.GRANTED).build());

        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertTrue(result.has("isConfidential"));
        verifyNoMoreInteractions(ccdService);
    }

    @Test
    public void givenDwpRaiseException_thenSetDigitalFlagNoToRobotics() {
        caseData.setCreatedInGapsFrom(VALID_APPEAL.getId());
        caseData.setIsProgressingViaGaps("Yes");

        caseDetails = new CaseDetails<>(1234L, "sscs", State.NOT_LISTABLE, caseData, null, "Benefit");

        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertEquals("No", result.getString("isDigital"));
        verifyNoMoreInteractions(ccdService);
    }

    @Test
    public void givenSscsCaseDataPip_makeValidRoboticsJsonThatHasRightDwpIssueOffice() {
        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertTrue(result.has("hearingArrangements"));
        assertTrue(result.has("isReadyToList"));
        assertThat(result.get("dwpIssuingOffice"), is("DWP PIP (1)"));

        verifyNoMoreInteractions(ccdService);
    }

    @Test
    public void givenSscsCaseDataEsa_makeValidRoboticsJsonThatHasRightDwpIssueOffice() {
        caseDetails.getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("ESA").build());
        caseDetails.getCaseData().getAppeal().setMrnDetails(MrnDetails.builder().dwpIssuingOffice("Milton Keynes DRT").build());

        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertTrue(result.has("hearingArrangements"));
        assertTrue(result.has("isReadyToList"));
        assertThat(result.get("dwpIssuingOffice"), is("Sheffield Benefit Centre"));

        verifyNoMoreInteractions(ccdService);
    }

    @Test
    public void givenSscsCaseDataUc_makeValidRoboticsJsonThatHasRightDwpIssueOffice() {
        caseDetails.getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("UC").build());
        caseDetails.getCaseData().getAppeal().setMrnDetails(MrnDetails.builder().dwpIssuingOffice("Universal Credit").build());

        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertTrue(result.has("hearingArrangements"));
        assertTrue(result.has("isReadyToList"));
        assertThat(result.get("dwpIssuingOffice"), is("Universal Credit"));

        verifyNoMoreInteractions(ccdService);
    }

    @Test
    public void givenSscsCaseDataDla_makeValidRoboticsJsonThatHasRightDwpIssueOffice() {
        caseDetails.getCaseData().getAppeal().setBenefitType(BenefitType.builder().code("DLA").build());
        caseDetails.getCaseData().getAppeal().setMrnDetails(MrnDetails.builder().dwpIssuingOffice("The Pension Service 11").build());

        JSONObject result = roboticsService.sendCaseToRobotics(caseDetails);

        assertThat(result.get("caseId"), is(1234L));
        assertTrue(result.has("appellant"));
        assertTrue(result.has("hearingArrangements"));
        assertTrue(result.has("isReadyToList"));
        assertThat(result.get("dwpIssuingOffice"), is("Attendance Allowance & DLA65 DRT"));

        verifyNoMoreInteractions(ccdService);
    }
}
