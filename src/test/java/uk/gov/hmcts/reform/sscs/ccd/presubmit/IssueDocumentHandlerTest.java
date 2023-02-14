package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.Respondent;

@RunWith(JUnitParamsRunner.class)
public class IssueDocumentHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private IssueDocumentHandler handler;

    @Before
    public void setUp() {
        handler = new IssueDocumentHandler();
    }

    @Test
    @Parameters({"CHILD_SUPPORT", "TAX_CREDIT"})
    public void givenAnSscs2OrSscs5BenefitType_thenHideNino(Benefit benefit) {
        assertTrue(handler.isBenefitTypeValidToHideNino(Optional.ofNullable(benefit)));
    }

    @Test
    public void givenAnSscs1BenefitType_thenDoNotHideNino() {
        assertFalse(handler.isBenefitTypeValidToHideNino(Optional.ofNullable(PIP)));
    }

    @Parameters({"PIP", "ESA", "UC", "JSA", "DLA", "UC", "carersAllowance", "attendanceAllowance",
        "bereavementBenefit", "industrialInjuriesDisablement", "maternityAllowance", "socialFund",
        "incomeSupport", "bereavementSupportPaymentScheme", "industrialDeathBenefit", "pensionCredit", "retirementPension"})
    public void givenAnSscs1BenefitType_thenAddDwpAsRespondent(String benefit) {
        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefit).build()).build()).build();
        List<Respondent> respondents = handler.getRespondents(sscsCaseData);
        assertEquals(1, respondents.size());
        assertEquals("Respondent: Secretary of State for Work and Pensions", respondents.get(0).getName());
    }

    @Test
    public void givenAnSscs2BenefitType_thenAddDwpAsRespondent() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build()).build();

        List<Respondent> respondents = handler.getRespondents(sscsCaseData);
        assertEquals(1, respondents.size());
        assertEquals("Respondent: Secretary of State for Work and Pensions", respondents.get(0).getName());
    }

    @Test
    @Parameters({"taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection", "childBenefit",
        "thirtyHoursFreeChildcare", "guaranteedMinimumPension", "nationalInsuranceCredits"})
    public void givenAnSscs5BenefitType_thenAddHmrcAsRespondent(String benefit) {
        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefit).build()).build()).build();

        List<Respondent> respondents = handler.getRespondents(sscsCaseData);
        assertEquals(1, respondents.size());
        assertEquals("Respondent: HM Revenue & Customs", respondents.get(0).getName());
    }

    @Test
    public void givenOtherParties_thenAddAsRespondent() {
        CcdValue<OtherParty> otherParty1 = CcdValue.<OtherParty>builder()
                .value(OtherParty.builder().id("1").name(Name.builder().title("Mr").firstName("Hugo").lastName("Lloris").build()).build()).build();
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build())
                .otherParties(singletonList(otherParty1)).build();

        List<Respondent> respondents = handler.getRespondents(sscsCaseData);
        assertEquals(2, respondents.size());
        assertEquals("Respondent: Secretary of State for Work and Pensions", respondents.get(0).getName());
        assertEquals("Second Respondent: Mr Hugo Lloris", respondents.get(1).getName());
    }

    @Test
    public void given10OtherParties_thenAddAsRespondentWithoutNumber() {
        CcdValue<OtherParty> otherParty1 = CcdValue.<OtherParty>builder()
                .value(OtherParty.builder().id("1").name(Name.builder().title("Mr").firstName("Hugo").lastName("Lloris").build()).build()).build();
        List<CcdValue<OtherParty>> otherParties = asList(otherParty1, otherParty1, otherParty1, otherParty1, otherParty1,
                otherParty1, otherParty1, otherParty1, otherParty1, otherParty1, otherParty1);
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build())
                .otherParties(otherParties).build();

        List<Respondent> respondents = handler.getRespondents(sscsCaseData);
        assertEquals(12, respondents.size());
        assertEquals("Respondent: Secretary of State for Work and Pensions", respondents.get(0).getName());
        assertEquals("Second Respondent: Mr Hugo Lloris", respondents.get(1).getName());
        assertEquals("Respondent: Mr Hugo Lloris", respondents.get(11).getName());
    }

    @Test
    public void testDocumentPayloadValues() {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .documentGeneration(DocumentGeneration.builder()
                .bodyContent("Hello World")
                .signedBy("Barry Allen")
                .signedRole("Judge")
                .build())
                .ccdCaseId("1")
                .appeal(Appeal.builder()
                    .appellant(Appellant.builder()
                        .name(Name.builder()
                            .title("Mr")
                            .firstName("User")
                            .lastName("Lloris")
                            .build())
                            .identity(Identity.builder()
                                .nino("BB 22 55 66 B")
                                .build())
                            .build())
                    .benefitType(BenefitType.builder()
                        .code("PIP")
                        .build())
                    .signer("Signer")
                    .hearingType("oral")
                    .receivedVia("Online")
                    .build())
                .build();

        String documentTypeLabel = "directions notice";
        LocalDate localDate = LocalDate.now();
        NoticeIssuedTemplateBody payload = handler.createPayload(null, sscsCaseData, documentTypeLabel, localDate, localDate, false, USER_AUTHORISATION);
        assertEquals("User Lloris", payload.getAppellantFullName());
        assertNull(payload.getAppointeeFullName());
        assertEquals("1", payload.getCaseId());
        assertEquals("BB 22 55 66 B", payload.getNino());
        assertFalse(payload.isShouldHideNino());
        assertEquals(1, payload.getRespondents().size());
        assertEquals("Hello World", payload.getNoticeBody());
        assertEquals("Barry Allen", payload.getUserName());
        assertEquals("DIRECTIONS NOTICE", payload.getNoticeType());
        assertEquals("Judge", payload.getUserRole());
        assertEquals(localDate, payload.getDateAdded());
        assertEquals(localDate, payload.getGeneratedDate());
        assertEquals("Barry Allen", payload.getIdamSurname());
    }

    @ParameterizedTest
    @EnumSource(value = SetAsideActions.class, names = {"GRANT", "REFUSE"})
    public void givenSetAsideState_thenReturnSetAsideDecisionNotice(SetAsideActions setAsideActions) {
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("1")
            .postHearing(PostHearing.builder()
                .setAside(SetAside.builder()
                    .action(setAsideActions)
                    .build())
                .build())
            .build();

        String documentTypeLabel = new IssueDocumentHandler().getDocumentTypeLabel(sscsCaseData, DocumentType.DECISION_NOTICE, "adfdsf2");

        assertEquals("Set Aside Decision Notice", documentTypeLabel);
    }

    @Test
    public void givenSetAsideStateIsNull_thenReturnDraftDecisionNotice() {
        String expectedDefaultDocumentLabel = "Draft Decision Notice";
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("1")
            .postHearing(PostHearing.builder()
                .setAside(SetAside.builder()
                    .action(null)
                    .build())
               .build())
            .build();

        String documentTypeLabel = new IssueDocumentHandler().getDocumentTypeLabel(sscsCaseData, DocumentType.DECISION_NOTICE, "Draft Decision Notice");
        assertEquals(expectedDefaultDocumentLabel, documentTypeLabel);
    }

    @Test
    public void givenHearingIsNull_thenReturnDraftDecisionNotice() {
        String expectedDefaultDocumentLabel = "Draft Decision Notice";
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("1")
            .build();

        String documentTypeLabel = new IssueDocumentHandler().getDocumentTypeLabel(sscsCaseData, DocumentType.DECISION_NOTICE, "Draft Decision Notice");
        assertEquals(expectedDefaultDocumentLabel, documentTypeLabel);
    }

}
