package uk.gov.hmcts.reform.sscs.ccd.presubmit;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static uk.gov.hmcts.reform.sscs.ccd.domain.Benefit.PIP;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import uk.gov.hmcts.reform.sscs.ccd.callback.DocumentType;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.docassembly.NoticeIssuedTemplateBody;
import uk.gov.hmcts.reform.sscs.model.docassembly.Respondent;

class IssueDocumentHandlerTest {

    private static final String USER_AUTHORISATION = "Bearer token";

    private IssueDocumentHandler handler;

    @BeforeEach
    void setUp() {
        handler = new IssueDocumentHandler();
    }

    @ParameterizedTest
    @EnumSource(value = Benefit.class, names = {"CHILD_SUPPORT", "TAX_CREDIT"})
    void givenAnSscs2OrSscs5BenefitType_thenHideNino(Benefit benefit) {
        assertThat(handler.isBenefitTypeValidToHideNino(Optional.ofNullable(benefit))).isTrue();
    }

    @Test
    void givenAnSscs1BenefitType_thenDoNotHideNino() {
        assertThat(handler.isBenefitTypeValidToHideNino(Optional.of(PIP))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PIP", "ESA", "UC", "JSA", "DLA", "UC", "carersAllowance", "attendanceAllowance",
        "bereavementBenefit", "industrialInjuriesDisablement", "maternityAllowance", "socialFund",
        "incomeSupport", "bereavementSupportPaymentScheme", "industrialDeathBenefit", "pensionCredit", "retirementPension"})
    void givenAnSscs1BenefitType_thenAddDwpAsRespondent(String benefit) {
        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefit).build()).build()).build();
        List<Respondent> respondents = handler.getRespondents(sscsCaseData);
        assertThat(respondents).hasSize(1);
        assertThat(respondents.get(0).getName()).isEqualTo("Respondent: Secretary of State for Work and Pensions");
    }

    @Test
    void givenAnSscs2BenefitType_thenAddDwpAsRespondent() {
        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build()).build();

        List<Respondent> respondents = handler.getRespondents(sscsCaseData);
        assertThat(respondents).hasSize(1);
        assertThat(respondents.get(0).getName()).isEqualTo("Respondent: Secretary of State for Work and Pensions");
    }

    @ParameterizedTest
    @ValueSource(strings = {"taxCredit", "guardiansAllowance", "taxFreeChildcare", "homeResponsibilitiesProtection", "childBenefit",
        "thirtyHoursFreeChildcare", "guaranteedMinimumPension", "nationalInsuranceCredits"})
    void givenAnSscs5BenefitType_thenAddHmrcAsRespondent(String benefit) {
        SscsCaseData sscsCaseData = SscsCaseData.builder().appeal(Appeal.builder().benefitType(BenefitType.builder().code(benefit).build()).build()).build();

        List<Respondent> respondents = handler.getRespondents(sscsCaseData);
        assertThat(respondents).hasSize(1);
        assertThat(respondents.get(0).getName()).isEqualTo("Respondent: HM Revenue & Customs");
    }

    @Test
    void givenOtherParties_thenAddAsRespondent() {
        CcdValue<OtherParty> otherParty1 = CcdValue.<OtherParty>builder()
                .value(OtherParty.builder().id("1").name(Name.builder().title("Mr").firstName("Hugo").lastName("Lloris").build()).build()).build();
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build())
                .otherParties(singletonList(otherParty1)).build();

        List<Respondent> respondents = handler.getRespondents(sscsCaseData);
        assertThat(respondents).hasSize(2);
        assertThat(respondents.get(0).getName()).isEqualTo("Respondent: Secretary of State for Work and Pensions");
        assertThat(respondents.get(1).getName()).isEqualTo("Second Respondent: Mr Hugo Lloris");
    }

    @Test
    void given10OtherParties_thenAddAsRespondentWithoutNumber() {
        CcdValue<OtherParty> otherParty1 = CcdValue.<OtherParty>builder()
                .value(OtherParty.builder().id("1").name(Name.builder().title("Mr").firstName("Hugo").lastName("Lloris").build()).build()).build();
        List<CcdValue<OtherParty>> otherParties = asList(otherParty1, otherParty1, otherParty1, otherParty1, otherParty1,
                otherParty1, otherParty1, otherParty1, otherParty1, otherParty1, otherParty1);
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .appeal(Appeal.builder().benefitType(BenefitType.builder().code("childSupport").build()).build())
                .otherParties(otherParties).build();

        List<Respondent> respondents = handler.getRespondents(sscsCaseData);
        assertThat(respondents).hasSize(12);
        assertThat(respondents.get(0).getName()).isEqualTo("Respondent: Secretary of State for Work and Pensions");
        assertThat(respondents.get(1).getName()).isEqualTo("Second Respondent: Mr Hugo Lloris");
        assertThat(respondents.get(11).getName()).isEqualTo("Respondent: Mr Hugo Lloris");
    }

    private SscsCaseData buildCaseData() {
        return SscsCaseData.builder()
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
    }

    @Test
    void testDocumentPayloadValues() {
        SscsCaseData sscsCaseData = buildCaseData();

        String documentTypeLabel = "directions notice";
        LocalDate localDate = LocalDate.now();
        NoticeIssuedTemplateBody payload = handler.createPayload(null, sscsCaseData, documentTypeLabel, localDate, localDate, false, false, false, USER_AUTHORISATION);
        assertThat(payload.getAppellantFullName()).isEqualTo("User Lloris");
        assertThat(payload.getAppointeeFullName()).isNull();
        assertThat(payload.getCaseId()).isEqualTo("1");
        assertThat(payload.getNino()).isEqualTo("BB 22 55 66 B");
        assertThat(payload.isShouldHideNino()).isFalse();
        assertThat(payload.getRespondents()).hasSize(1);
        assertThat(payload.getNoticeBody()).isEqualTo("Hello World");
        assertThat(payload.getUserName()).isEqualTo("Barry Allen");
        assertThat(payload.getNoticeType()).isEqualTo("DIRECTIONS NOTICE");
        assertThat(payload.getUserRole()).isEqualTo("Judge");
        assertThat(payload.getDateAdded()).isEqualTo(localDate);
        assertThat(payload.getGeneratedDate()).isEqualTo(localDate);
        assertThat(payload.getIdamSurname()).isEqualTo("Barry Allen");
    }

    @Test
    void givenSetAsideState_andPostHearingsIsEnabled_thenReturnSetAsideDecisionNotice() {
        final String originalLabel = "label";
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1")
                .postHearing(PostHearing.builder()
                    .reviewType(PostHearingReviewType.SET_ASIDE)
                    .build())
                .build();

        boolean isPostHearingsEnabled = true;
        String documentTypeLabel = new IssueDocumentHandler().getEmbeddedDocumentTypeLabel(sscsCaseData, DocumentType.DECISION_NOTICE, originalLabel, isPostHearingsEnabled);

        String expectedLabel = "Set Aside Decision Notice";
        assertThat(documentTypeLabel).isEqualTo(expectedLabel);
    }

    @Test
    void givenSetAsideState_andPostHearingsIsDisabled_thenReturnOriginalLabel() {
        final String originalLabel = "label";
        SscsCaseData sscsCaseData = SscsCaseData.builder()
                .ccdCaseId("1")
                .postHearing(PostHearing.builder()
                    .reviewType(PostHearingReviewType.SET_ASIDE)
                .build())
            .build();

        boolean isPostHearingsEnabled = false;
        String documentTypeLabel = new IssueDocumentHandler().getEmbeddedDocumentTypeLabel(sscsCaseData, DocumentType.DECISION_NOTICE, originalLabel, isPostHearingsEnabled);

        assertThat(documentTypeLabel).isEqualTo(originalLabel);
    }

    @Test
    void givenSetAsideStateIsNull_thenReturnDraftDecisionNotice() {
        String expectedDefaultDocumentLabel = "Draft Decision Notice";
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("1")
            .postHearing(PostHearing.builder()
                .setAside(SetAside.builder()
                    .action(null)
                    .build())
               .build())
            .build();

        String documentTypeLabel = new IssueDocumentHandler().getEmbeddedDocumentTypeLabel(sscsCaseData, DocumentType.DECISION_NOTICE, expectedDefaultDocumentLabel, false);
        assertThat(documentTypeLabel).isEqualTo(expectedDefaultDocumentLabel);
    }

    @Test
    void givenHearingIsNull_thenReturnDraftDecisionNotice() {
        String expectedDefaultDocumentLabel = "Draft Decision Notice";
        SscsCaseData sscsCaseData = SscsCaseData.builder()
            .ccdCaseId("1")
            .build();

        String documentTypeLabel = new IssueDocumentHandler().getEmbeddedDocumentTypeLabel(sscsCaseData, DocumentType.DECISION_NOTICE, expectedDefaultDocumentLabel, false);
        assertThat(documentTypeLabel).isEqualTo(expectedDefaultDocumentLabel);
    }

    @Test
    void givenPostHearingReviewIsSetAside_thenUseBodyContent() {
        SscsCaseData sscsCaseData = buildCaseData();
        sscsCaseData.getPostHearing().setReviewType(PostHearingReviewType.SET_ASIDE);
        String bodyContent = "set aside body content";
        sscsCaseData.getDocumentGeneration().setBodyContent(bodyContent);

        NoticeIssuedTemplateBody payload = handler.createPayload(null, sscsCaseData, "doctype", LocalDate.now(), LocalDate.now(), false, true, false, USER_AUTHORISATION);

        assertThat(payload.getNoticeBody()).isEqualTo(bodyContent);
    }

    @Test
    void givenPostHearingReviewIsCorrection_thenUseCorrectionBody() {
        SscsCaseData sscsCaseData = buildCaseData();
        sscsCaseData.getPostHearing().setReviewType(PostHearingReviewType.CORRECTION);
        String bodyContent = "correction body content";
        sscsCaseData.getDocumentGeneration().setCorrectionBodyContent(bodyContent);

        NoticeIssuedTemplateBody payload = handler.createPayload(null, sscsCaseData, "doctype", LocalDate.now(), LocalDate.now(), false, true, false, USER_AUTHORISATION);

        assertThat(payload.getNoticeBody()).isEqualTo(bodyContent);
    }

    @Test
    void givenPostHearingReviewIsSor_thenUseSorBody() {
        SscsCaseData sscsCaseData = buildCaseData();
        sscsCaseData.getPostHearing().setReviewType(PostHearingReviewType.STATEMENT_OF_REASONS);
        String bodyContent = "sor body content";
        sscsCaseData.getDocumentGeneration().setStatementOfReasonsBodyContent(bodyContent);

        NoticeIssuedTemplateBody payload = handler.createPayload(null, sscsCaseData, "doctype", LocalDate.now(), LocalDate.now(), false, true, false, USER_AUTHORISATION);

        assertThat(payload.getNoticeBody()).isEqualTo(bodyContent);
    }

    @Test
    void givenPostHearingReviewIsLta_thenUseLtaBody() {
        SscsCaseData sscsCaseData = buildCaseData();
        sscsCaseData.getPostHearing().setReviewType(PostHearingReviewType.LIBERTY_TO_APPLY);
        String bodyContent = "lta body content";
        sscsCaseData.getDocumentGeneration().setLibertyToApplyBodyContent(bodyContent);

        NoticeIssuedTemplateBody payload = handler.createPayload(null, sscsCaseData, "doctype", LocalDate.now(), LocalDate.now(), false, true, true, USER_AUTHORISATION);

        assertThat(payload.getNoticeBody()).isEqualTo(bodyContent);
    }

    @Test
    void givenPostHearingReviewIsPta_thenUsePtaBody() {
        SscsCaseData sscsCaseData = buildCaseData();
        sscsCaseData.getPostHearing().setReviewType(PostHearingReviewType.PERMISSION_TO_APPEAL);
        String bodyContent = "pta body content";
        sscsCaseData.getDocumentGeneration().setPermissionToAppealBodyContent(bodyContent);

        NoticeIssuedTemplateBody payload = handler.createPayload(null, sscsCaseData, "doctype", LocalDate.now(), LocalDate.now(), false, true, true, USER_AUTHORISATION);

        assertThat(payload.getNoticeBody()).isEqualTo(bodyContent);
    }

    @EnabledIf("postHearingReviewTypeHasMoreThan5Values")
    @ParameterizedTest
    @EnumSource(
        value = PostHearingReviewType.class,
        names = {
            "SET_ASIDE",
            "CORRECTION",
            "STATEMENT_OF_REASONS",
            "LIBERTY_TO_APPLY",
            "PERMISSION_TO_APPEAL"
            // TODO add post hearings B types as they are implemented
        },
        mode = EnumSource.Mode.EXCLUDE
    )
    void givenPostHearingReviewTypeIsNotImplemented_thenThrowException(PostHearingReviewType postHearingReviewType) {
        SscsCaseData sscsCaseData = buildCaseData();
        sscsCaseData.getPostHearing().setReviewType(postHearingReviewType);

        assertThatThrownBy(() ->
            handler.createPayload(null, sscsCaseData, "doctype", LocalDate.now(), LocalDate.now(), false, true, true, USER_AUTHORISATION))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("caseData has unexpected postHearingReviewType: " + postHearingReviewType.getDescriptionEn());
    }

    static boolean postHearingReviewTypeHasMoreThan5Values() {
        return PostHearingReviewType.values().length > 5;
    }
}
