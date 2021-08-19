package uk.gov.hmcts.reform.sscs.service.converter;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;
import uk.gov.hmcts.reform.sscs.model.draft.SessionHaveAMrn;
import uk.gov.hmcts.reform.sscs.model.draft.SessionHaveContactedDwp;
import uk.gov.hmcts.reform.sscs.model.draft.SessionHearingArrangement;
import uk.gov.hmcts.reform.sscs.model.draft.SessionHearingArrangements;
import uk.gov.hmcts.reform.sscs.model.draft.SessionHearingArrangementsSelection;
import uk.gov.hmcts.reform.sscs.model.draft.SessionNoMrn;
import uk.gov.hmcts.reform.sscs.service.DocumentDownloadService;
import uk.gov.hmcts.reform.sscs.transform.deserialize.HearingOptionArrangements;

@RunWith(JUnitParamsRunner.class)
@SpringBootTest(classes = ConvertSscsCaseDataIntoSessionDraft.class)
public class ConvertSscsCaseDataIntoSessionDraftTest {
    @ClassRule
    public static final SpringClassRule SCR = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    private SscsCaseData caseData;

    @MockBean
    private DocumentDownloadService documentDownloadService;
    @Autowired
    private ConvertSscsCaseDataIntoSessionDraft convertSscsCaseDataIntoSessionDraft;
    private SessionDraft actual;

    @Test(expected = NullPointerException.class)
    public void attemptToConvertNull() {
        convertSscsCaseDataIntoSessionDraft.convert(null);
    }

    @Test(expected = NullPointerException.class)
    public void attemptToConvertNullAppeal() {
        SscsCaseData caseData = SscsCaseData.builder().build();
        convertSscsCaseDataIntoSessionDraft.convert(caseData);
    }

    @Test
    @Parameters(method = "getSscsDocumentScenarios")
    public void givenEvidenceDescriptionIsProvided_shouldReturnSessionEvidenceDescription(
        List<SscsDocument> sscsDocumentList) {

        caseData = SscsCaseData.builder()
            .appeal(Appeal.builder().build())
            .sscsDocument(sscsDocumentList)
            .build();

        given(documentDownloadService.getFileSize(anyString())).willReturn(1L);

        SessionDraft actual = convertSscsCaseDataIntoSessionDraft.convert(caseData);

        assertEquals("my evidence description", actual.getEvidenceDescription().getDescribeTheEvidence());
    }

    private Object[] getSscsDocumentScenarios() {
        List<SscsDocument> sscsDocumentList = Collections.singletonList(SscsDocument.builder()
            .value(SscsDocumentDetails.builder()
                .documentComment("my evidence description")
                .documentLink(DocumentLink.builder()
                    .documentBinaryUrl("https://documentLink")
                    .build())
                .build())
            .build());

        return new Object[]{
            new Object[]{sscsDocumentList}
        };
    }


    @Test
    public void givenDwpIssuingOfficeEsa_shouldReturnResponseWithDwpIssuingOfficeEsa() {
        caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("ESA")
                    .description("Employment and Support Allowance")
                    .build())
                .mrnDetails(MrnDetails.builder()
                    .mrnDate("2019-05-01")
                    .dwpIssuingOffice("any office")
                    .build())
                .build())
            .build();

        SessionDraft actualSessionDraft = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertEquals("any office", actualSessionDraft.getDwpIssuingOfficeEsa().getDwpIssuingOffice());
        assertEquals("Employment and Support Allowance (ESA)",
            actualSessionDraft.getBenefitType().getBenefitType());
        assertEquals("1", actualSessionDraft.getMrnDate().getMrnDateDetails().getDay());
        assertEquals("5", actualSessionDraft.getMrnDate().getMrnDateDetails().getMonth());
        assertEquals("2019", actualSessionDraft.getMrnDate().getMrnDateDetails().getYear());
    }

    @Test
    public void givenBenefitType_shouldReturnResponseBenefitTypeStringWithoutCode() {
        caseData = SscsCaseData.builder()
                .appeal(Appeal.builder()
                        .benefitType(BenefitType.builder()
                                .code("bereavementBenefit")
                                .description("Bereavement Benefit")
                                .build())
                        .build())
                .build();

        SessionDraft actualSessionDraft = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertEquals("Bereavement Benefit",
                actualSessionDraft.getBenefitType().getBenefitType());
    }

    @Test
    @Parameters(method = "generateMrnLateScenarios")
    public void givenMrnIsLate_shouldResponseWithCorrectMrnLateResponse(
        MrnDetails mrnDetails, String expectedReason) {

        caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .mrnDetails(mrnDetails)
                .build())
            .build();

        SessionDraft actualSessionDraft = convertSscsCaseDataIntoSessionDraft.convert(caseData);

        if (actualSessionDraft.getMrnOverThirteenMonthsLate() != null) {
            assertEquals(expectedReason, actualSessionDraft.getMrnOverThirteenMonthsLate().getReasonForBeingLate());
            assertNull(actualSessionDraft.getMrnOverOneMonthLate());
        }
        if (actualSessionDraft.getMrnOverOneMonthLate() != null) {
            assertEquals(expectedReason, actualSessionDraft.getMrnOverOneMonthLate().getReasonForBeingLate());
            assertNull(actualSessionDraft.getMrnOverThirteenMonthsLate());
        }
    }

    @SuppressWarnings({"unused"})
    private Object[] generateMrnLateScenarios() {
        MrnDetails mrnDetailsOverThirteenMonthsLate = MrnDetails.builder()
            .mrnLateReason("thirteen late reasons")
            .mrnDate("2017-03-01")
            .dwpIssuingOffice(null)
            .mrnMissingReason("")
            .build();

        MrnDetails mrnDetailsIsOverOneMonthsLate = MrnDetails.builder()
            .mrnLateReason("one month late reason")
            .mrnDate(LocalDate.now().minusMonths(2L).toString())
            .dwpIssuingOffice(null)
            .mrnMissingReason("")
            .build();

        MrnDetails mrnDetailsIsNull = MrnDetails.builder()
            .mrnLateReason(null)
            .mrnDate(null)
            .dwpIssuingOffice(null)
            .mrnMissingReason(null)
            .build();

        return new Object[]{
            new Object[]{mrnDetailsOverThirteenMonthsLate, mrnDetailsOverThirteenMonthsLate.getMrnLateReason()},
            new Object[]{mrnDetailsIsOverOneMonthsLate, mrnDetailsIsOverOneMonthsLate.getMrnLateReason()},
            new Object[]{mrnDetailsIsNull, null}
        };
    }

    @Test
    @Parameters(method = "generateMrnPossibleScenarios")
    public void givenMrnPathIsCompleted_shouldBuildHaveMrnSessionObjectCorrectly(MrnDetails mrnDetails,
                                                                                 SessionDraft expectedSessionDraft) {
        caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .mrnDetails(mrnDetails)
                .build())
            .build();

        SessionDraft actualSessionDraft = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertEquals(actualSessionDraft.getMrnDate(), expectedSessionDraft.getMrnDate());
        assertEquals(actualSessionDraft.getNoMrn(), expectedSessionDraft.getNoMrn());
        assertEquals(actualSessionDraft.getHaveAMrn(), expectedSessionDraft.getHaveAMrn());
        assertEquals(actualSessionDraft.getHaveContactedDwp(), expectedSessionDraft.getHaveContactedDwp());

    }

    @SuppressWarnings({"unused"})
    private Object[] generateMrnPossibleScenarios() {
        MrnDetails mrnDetailsIsNull = MrnDetails.builder()
            .mrnLateReason(null)
            .mrnDate(null)
            .dwpIssuingOffice(null)
            .mrnMissingReason(null)
            .build();

        SessionDraft sessionDraftExpectedWhenMrnDetailsIsNull = SessionDraft.builder()
            .mrnDate(null)
            .haveAMrn(null)
            .noMrn(null)
            .haveContactedDwp(null)
            .build();


        MrnDetails mrnDetailsWithMissionReason = MrnDetails.builder()
            .mrnLateReason(null)
            .mrnDate(null)
            .dwpIssuingOffice(null)
            .mrnMissingReason("reason for no Mrn")
            .build();

        SessionDraft sessionDraftExpectedWhenHaveNoMrnAndHaveContactedDwp = SessionDraft.builder()
            .mrnDate(null)
            .haveAMrn(new SessionHaveAMrn("no"))
            .noMrn(new SessionNoMrn("reason for no Mrn"))
            .haveContactedDwp(new SessionHaveContactedDwp("yes"))
            .build();

        return new Object[]{
            new Object[]{mrnDetailsIsNull, sessionDraftExpectedWhenMrnDetailsIsNull},
            new Object[]{mrnDetailsWithMissionReason, sessionDraftExpectedWhenHaveNoMrnAndHaveContactedDwp}
        };
    }

    @Test(expected = NullPointerException.class)
    public void convertNullCaseData() {
        convertSscsCaseDataIntoSessionDraft.convert(null);
    }

    @Test(expected = NullPointerException.class)
    public void convertCaseDataWithNullAppeal() {
        caseData = SscsCaseData.builder()
            .appeal(null)
            .build();

        convertSscsCaseDataIntoSessionDraft.convert(caseData);
    }

    @Test
    public void convertPopulatedCaseData() {
        caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("PIP")
                    .description("Personal Independence Payment")
                    .build()
                )
                .appellant(Appellant.builder()
                    .name(Name.builder()
                        .title("Mrs.")
                        .firstName("Ap")
                        .lastName("Pellant")
                        .build()
                    )
                    .identity(Identity.builder()
                        .dob("1998-12-31")
                        .nino("SC 94 27 06 A")
                        .build()
                    )
                    .address(Address.builder()
                        .line1("1 Appellant Close")
                        .town("Appellant-town")
                        .county("Appellant-county")
                        .postcode("TS1 1ST")
                        .build()
                    )
                    .contact(Contact.builder()
                        .mobile("07911123456")
                        .email("appellant@gmail.com")
                        .build()
                    )
                    .isAppointee("No")
                    .isAddressSameAsAppointee("No")
                    .build()
                )
                .mrnDetails(MrnDetails.builder()
                    .mrnDate("2010-02-01")
                    .mrnLateReason("Forgot to send it")
                    .dwpIssuingOffice("DWP PIP (1)")
                    .build()
                )
                .appealReasons(
                    AppealReasons.builder()
                        .reasons(
                            Collections.singletonList(
                                AppealReason.builder()
                                    .value(AppealReasonDetails.builder()
                                        .reason("Underpayment")
                                        .description("I think I should get more")
                                        .build()
                                    )
                                    .build()
                            )
                        )
                        .otherReasons("I can't think of anything else")
                        .build()
                )
                .hearingOptions(null)
                .rep(Representative.builder().build())
                .build()
            )
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(Subscription.builder()
                    .subscribeEmail("Yes")
                    .email("appellant@gmail.com")
                    .subscribeSms("Yes")
                    .mobile("07911123456")
                    .build()
                )
                .build()
            )
            .sscsDocument(Collections.emptyList())
            .evidencePresent("no")
            .ccdCaseId("123456")
            .build();

        SessionDraft actual = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertEquals("Personal Independence Payment (PIP)", actual.getBenefitType().getBenefitType());
        assertEquals("yes", actual.getCreateAccount().getCreateAccount());
        assertEquals("yes", actual.getHaveAMrn().getHaveAMrn());
        assertEquals("1", actual.getMrnDate().getMrnDateDetails().getDay());
        assertEquals("2", actual.getMrnDate().getMrnDateDetails().getMonth());
        assertEquals("2010", actual.getMrnDate().getMrnDateDetails().getYear());
        assertEquals("1", actual.getDwpIssuingOffice().getPipNumber());
        assertEquals("no", actual.getAppointee().getIsAppointee());
        assertEquals("Mrs.", actual.getAppellantName().getTitle());
        assertEquals("Ap", actual.getAppellantName().getFirstName());
        assertEquals("Pellant", actual.getAppellantName().getLastName());
        assertEquals("31", actual.getAppellantDob().getDate().getDay());
        assertEquals("12", actual.getAppellantDob().getDate().getMonth());
        assertEquals("1998", actual.getAppellantDob().getDate().getYear());
        assertEquals("SC 94 27 06 A", actual.getAppellantNino().getNino());
        assertEquals("1 Appellant Close", actual.getAppellantContactDetails().getAddressLine1());
        assertNull(actual.getAppellantContactDetails().getAddressLine2());
        assertEquals("Appellant-town", actual.getAppellantContactDetails().getTownCity());
        assertEquals("Appellant-county", actual.getAppellantContactDetails().getCounty());
        assertEquals("TS1 1ST", actual.getAppellantContactDetails().getPostCode());
        assertEquals("07911123456", actual.getAppellantContactDetails().getPhoneNumber());
        assertEquals("appellant@gmail.com", actual.getAppellantContactDetails().getEmailAddress());
        assertEquals("no", actual.getSameAddress().getIsAddressSameAsAppointee());
        assertEquals("yes", actual.getTextReminders().getDoYouWantTextMsgReminders());
        assertEquals("yes", actual.getSendToNumber().getUseSameNumber());
        assertNull("no", actual.getRepresentative());
        assertEquals("I think I should get more", actual.getReasonForAppealing().getReasonForAppealingItems().get(0).getReasonForAppealing());
        assertEquals("Underpayment", actual.getReasonForAppealing().getReasonForAppealingItems().get(0).getWhatYouDisagreeWith());
        assertEquals("I can't think of anything else", actual.getOtherReasonForAppealing().getOtherReasonForAppealing());
        assertEquals("no", actual.getEvidenceProvide().getEvidenceProvide());
        assertEquals("123456", actual.getCcdCaseId());
        assertNull(actual.getRepresentativeDetails());
    }

    @Test
    public void convertPopulatedCaseDataWithDifferentMobileNumber() {
        caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("PIP")
                    .description("Personal Independence Payment")
                    .build()
                )
                .appellant(Appellant.builder()
                    .name(Name.builder()
                        .title("Mrs.")
                        .firstName("Ap")
                        .lastName("Pellant")
                        .build()
                    )
                    .identity(Identity.builder()
                        .dob("1998-12-31")
                        .nino("SC 94 27 06 A")
                        .build()
                    )
                    .address(Address.builder()
                        .line1("1 Appellant Close")
                        .town("Appellant-town")
                        .county("Appellant-county")
                        .postcode("TS1 1ST")
                        .build()
                    )
                    .contact(Contact.builder()
                        .mobile("07911123456")
                        .email("appellant@gmail.com")
                        .build()
                    )
                    .isAppointee("No")
                    .isAddressSameAsAppointee("No")
                    .build()
                )
                .mrnDetails(MrnDetails.builder()
                    .mrnDate("2010-02-01")
                    .mrnLateReason("Forgot to send it")
                    .dwpIssuingOffice("DWP PIP (1)")
                    .build()
                )
                .appealReasons(
                    AppealReasons.builder()
                        .reasons(
                            Collections.singletonList(
                                AppealReason.builder()
                                    .value(AppealReasonDetails.builder()
                                        .reason("Underpayment")
                                        .description("I think I should get more")
                                        .build()
                                    )
                                    .build()
                            )
                        )
                        .otherReasons("I can't think of anything else")
                        .build()
                )
                .hearingOptions(null)
                .rep(Representative.builder().build())
                .build()
            )
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(Subscription.builder()
                    .subscribeEmail("Yes")
                    .email("appellant@gmail.com")
                    .subscribeSms("Yes")
                    .mobile("07911123999")
                    .build()
                )
                .build()
            )
            .sscsDocument(Collections.emptyList())
            .evidencePresent("no")
            .build();

        SessionDraft actual = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertEquals("07911123456", actual.getAppellantContactDetails().getPhoneNumber());
        assertEquals("yes", actual.getTextReminders().getDoYouWantTextMsgReminders());
        assertEquals("no", actual.getSendToNumber().getUseSameNumber());
    }

    @Test
    public void convertPopulatedCaseDataWithNoMrn() {
        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("PIP")
                    .description("Personal Independence Payment")
                    .build()
                )
                .appellant(Appellant.builder()
                    .name(Name.builder()
                        .title("Mrs.")
                        .firstName("Ap")
                        .lastName("Pellant")
                        .build()
                    )
                    .identity(Identity.builder()
                        .dob("1998-12-31")
                        .nino("SC 94 27 06 A")
                        .build()
                    )
                    .address(Address.builder()
                        .line1("1 Appellant Close")
                        .town("Appellant-town")
                        .county("Appellant-county")
                        .postcode("TS1 1ST")
                        .build()
                    )
                    .contact(Contact.builder()
                        .mobile("07911123456")
                        .email("appellant@gmail.com")
                        .build()
                    )
                    .isAppointee("No")
                    .build()
                )
                .mrnDetails(MrnDetails.builder()
                    .mrnMissingReason("I can't find the letter.")
                    .build()
                )
                .appealReasons(
                    AppealReasons.builder()
                        .reasons(
                            Collections.singletonList(
                                AppealReason.builder()
                                    .value(AppealReasonDetails.builder()
                                        .reason("Underpayment")
                                        .description("I think I should get more")
                                        .build()
                                    )
                                    .build()
                            )
                        )
                        .otherReasons("I can't think of anything else")
                        .build()
                )
                .hearingOptions(null)
                .rep(Representative.builder().build())
                .build()
            )
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(Subscription.builder()
                    .subscribeEmail("Yes")
                    .email("appellant@gmail.com")
                    .subscribeSms("Yes")
                    .mobile("07911123456")
                    .build()
                )
                .build()
            )
            .sscsDocument(Collections.emptyList())
            .evidencePresent("no")
            .build();

        SessionDraft actual = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertEquals("Personal Independence Payment (PIP)", actual.getBenefitType().getBenefitType());
        assertEquals("yes", actual.getCreateAccount().getCreateAccount());
        assertEquals("no", actual.getHaveAMrn().getHaveAMrn());
        assertEquals("I can't find the letter.", actual.getNoMrn().getReasonForNoMrn());
        assertEquals("no", actual.getAppointee().getIsAppointee());
        assertEquals("Mrs.", actual.getAppellantName().getTitle());
        assertEquals("Ap", actual.getAppellantName().getFirstName());
        assertEquals("Pellant", actual.getAppellantName().getLastName());
        assertEquals("31", actual.getAppellantDob().getDate().getDay());
        assertEquals("12", actual.getAppellantDob().getDate().getMonth());
        assertEquals("1998", actual.getAppellantDob().getDate().getYear());
        assertEquals("SC 94 27 06 A", actual.getAppellantNino().getNino());
        assertEquals("1 Appellant Close", actual.getAppellantContactDetails().getAddressLine1());
        assertNull(actual.getAppellantContactDetails().getAddressLine2());
        assertEquals("Appellant-town", actual.getAppellantContactDetails().getTownCity());
        assertEquals("Appellant-county", actual.getAppellantContactDetails().getCounty());
        assertEquals("TS1 1ST", actual.getAppellantContactDetails().getPostCode());
        assertEquals("07911123456", actual.getAppellantContactDetails().getPhoneNumber());
        assertEquals("appellant@gmail.com", actual.getAppellantContactDetails().getEmailAddress());
        assertEquals("yes", actual.getTextReminders().getDoYouWantTextMsgReminders());
        assertEquals("yes", actual.getSendToNumber().getUseSameNumber());
        assertNull("no", actual.getRepresentative());
        assertEquals("I think I should get more", actual.getReasonForAppealing().getReasonForAppealingItems().get(0).getReasonForAppealing());
        assertEquals("Underpayment", actual.getReasonForAppealing().getReasonForAppealingItems().get(0).getWhatYouDisagreeWith());
        assertEquals("I can't think of anything else", actual.getOtherReasonForAppealing().getOtherReasonForAppealing());
        assertEquals("no", actual.getEvidenceProvide().getEvidenceProvide());
        assertNull(actual.getRepresentativeDetails());
    }

    @Test
    public void convertPopulatedCaseDataWithAppointee() {
        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("PIP")
                    .description("Personal Independence Payment")
                    .build()
                )
                .appellant(Appellant.builder()
                    .address(Address.builder()
                        .postcode("AP1 14NT")
                        .build()
                    )
                    .appointee(Appointee.builder()
                        .name(Name.builder().title("Mr.").firstName("Ap").lastName("Pointee").build())
                        .identity(Identity.builder().dob("1999-01-01").build())
                        .contact(Contact.builder().mobile("07111111111").email("appointee@test.com").build())
                        .address(Address.builder()
                            .line1("1 Appointee Drive")
                            .town("Appointee-town")
                            .county("Appointee-county")
                            .postcode("TS2 2ST")
                            .build())
                        .build()
                    )
                    .isAppointee("Yes")
                    .isAddressSameAsAppointee("No")
                    .build()
                )
                .mrnDetails(MrnDetails.builder()
                    .mrnDate("2010-02-01")
                    .mrnLateReason("Forgot to send it")
                    .dwpIssuingOffice("DWP PIP (1)")
                    .build()
                )
                .rep(Representative.builder().build())
                .build()
            )
            .build();

        SessionDraft actual = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertEquals("yes", actual.getAppointee().getIsAppointee());
        assertEquals("Mr.", actual.getAppointeeName().getTitle());
        assertEquals("Ap", actual.getAppointeeName().getFirstName());
        assertEquals("Pointee", actual.getAppointeeName().getLastName());
        assertEquals("1", actual.getAppointeeDob().getDate().getDay());
        assertEquals("1", actual.getAppointeeDob().getDate().getMonth());
        assertEquals("1999", actual.getAppointeeDob().getDate().getYear());
        assertEquals("1 Appointee Drive", actual.getAppointeeContactDetails().getAddressLine1());
        assertNull(actual.getAppointeeContactDetails().getAddressLine2());
        assertEquals("Appointee-town", actual.getAppointeeContactDetails().getTownCity());
        assertEquals("Appointee-county", actual.getAppointeeContactDetails().getCounty());
        assertEquals("TS2 2ST", actual.getAppointeeContactDetails().getPostCode());
        assertEquals("07111111111", actual.getAppointeeContactDetails().getPhoneNumber());
        assertEquals("appointee@test.com", actual.getAppointeeContactDetails().getEmailAddress());
        assertEquals("no", actual.getSameAddress().getIsAddressSameAsAppointee());
    }

    @Test
    public void convertPopulatedCaseDataWithAppointeeAtSameAddress() {
        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("PIP")
                    .description("Personal Independence Payment")
                    .build()
                )
                .appellant(Appellant.builder()
                    .address(Address.builder()
                        .build()
                    )
                    .appointee(Appointee.builder()
                        .name(Name.builder().title("Mr.").firstName("Ap").lastName("Pointee").build())
                        .identity(Identity.builder().dob("1999-01-01").build())
                        .contact(Contact.builder().mobile("07111111111").email("appointee@test.com").build())
                        .address(Address.builder()
                            .line1("1 Appointee Drive")
                            .town("Appointee-town")
                            .county("Appointee-county")
                            .postcode("TS2 2ST")
                            .build())
                        .build()
                    )
                    .isAppointee("Yes")
                    .isAddressSameAsAppointee("Yes")
                    .build()
                )
                .mrnDetails(MrnDetails.builder()
                    .mrnDate("2010-02-01")
                    .mrnLateReason("Forgot to send it")
                    .dwpIssuingOffice("DWP PIP (1)")
                    .build()
                )
                .rep(Representative.builder().build())
                .build()
            )
            .build();

        SessionDraft actual = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertEquals("yes", actual.getAppointee().getIsAppointee());
        assertEquals("Mr.", actual.getAppointeeName().getTitle());
        assertEquals("Ap", actual.getAppointeeName().getFirstName());
        assertEquals("Pointee", actual.getAppointeeName().getLastName());
        assertEquals("1", actual.getAppointeeDob().getDate().getDay());
        assertEquals("1", actual.getAppointeeDob().getDate().getMonth());
        assertEquals("1999", actual.getAppointeeDob().getDate().getYear());
        assertEquals("1 Appointee Drive", actual.getAppointeeContactDetails().getAddressLine1());
        assertNull(actual.getAppointeeContactDetails().getAddressLine2());
        assertEquals("Appointee-town", actual.getAppointeeContactDetails().getTownCity());
        assertEquals("Appointee-county", actual.getAppointeeContactDetails().getCounty());
        assertEquals("TS2 2ST", actual.getAppointeeContactDetails().getPostCode());
        assertEquals("07111111111", actual.getAppointeeContactDetails().getPhoneNumber());
        assertEquals("appointee@test.com", actual.getAppointeeContactDetails().getEmailAddress());
        assertEquals("yes", actual.getSameAddress().getIsAddressSameAsAppointee());
    }

    @Test
    public void convertPopulatedCaseDataWithAppointeeNotSpecified() {
        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("PIP")
                    .description("Personal Independence Payment")
                    .build()
                )
                .appellant(Appellant.builder()
                    .address(Address.builder()
                        .postcode("AP1 14NT")
                        .build()
                    )
                    .appointee(Appointee.builder()
                        .name(Name.builder().firstName("Ap").lastName("Pointee").build())
                        .build()
                    )
                    .build()
                )
                .mrnDetails(MrnDetails.builder()
                    .mrnDate("2010-02-01")
                    .mrnLateReason("Forgot to send it")
                    .dwpIssuingOffice("DWP PIP (1)")
                    .build()
                )
                .rep(Representative.builder().build())
                .build()
            )
            .build();

        actual = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertNull(actual.getAppointee());
    }

    @Test
    @Parameters(method = "getDifferentRepsScenarios")
    public void givenRepsWithPostcodeLookUp_shouldReturnCorrectJson(Representative rep,
                                                                    String expectedPostcodeLookup,
                                                                    String expectedPostcodeAddress,
                                                                    String expectedType) {
        caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .rep(rep)
                .build())
            .build();

        actual = convertSscsCaseDataIntoSessionDraft.convert(caseData);

        assertEquals(expectedPostcodeLookup, actual.getRepresentativeDetails().getPostcodeLookup());
        assertEquals(expectedPostcodeAddress, actual.getRepresentativeDetails().getPostcodeAddress());
        assertEquals(expectedType, actual.getRepresentativeDetails().getType());
    }

    private Object[] getDifferentRepsScenarios() {
        Representative repWithNoLookupData = Representative.builder()
            .hasRepresentative("Yes")
            .name(Name.builder()
                .title("Miss.")
                .firstName("Re")
                .lastName("Presentative")
                .build())
            .contact(Contact.builder()
                .mobile("07333333333")
                .email("rep@gmail.com")
                .build())
            .address(Address.builder()
                .line1("1 Rep Cres")
                .town("Rep-town")
                .county("Rep-county")
                .postcode("TS3 3ST")
                .build())
            .build();

        Representative repWithAutoLookupData = Representative.builder()
            .hasRepresentative("Yes")
            .name(Name.builder()
                .title("Miss.")
                .firstName("Re")
                .lastName("Presentative")
                .build())
            .contact(Contact.builder()
                .mobile("07333333333")
                .email("rep@gmail.com")
                .build())
            .address(Address.builder()
                .line1("1 Rep Cres")
                .town("Rep-town")
                .county("Rep-county")
                .postcode("TS3 3ST")
                .postcodeAddress("234354")
                .postcodeLookup("TS3 3ST")
                .build())
            .build();

        Representative repWithManualLookupData = Representative.builder()
            .hasRepresentative("Yes")
            .name(Name.builder()
                .title("Miss.")
                .firstName("Re")
                .lastName("Presentative")
                .build())
            .contact(Contact.builder()
                .mobile("07333333333")
                .email("rep@gmail.com")
                .build())
            .address(Address.builder()
                .line1("1 Rep Cres")
                .town("Rep-town")
                .county("Rep-county")
                .postcode("TS3 3ST")
                .postcodeAddress("")
                .postcodeLookup("")
                .build())
            .build();

        return new Object[]{
            new Object[]{repWithNoLookupData, null, null, null},
            new Object[]{repWithAutoLookupData, "TS3 3ST", "234354", null},
            new Object[]{repWithManualLookupData, null, null, "manual"}
        };
    }

    @Test
    public void convertPopulatedCaseDataWithRep() {
        caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("PIP")
                    .description("Personal Independence Payment")
                    .build()
                )
                .appellant(Appellant.builder()
                    .name(Name.builder()
                        .title("Mrs.")
                        .firstName("Ap")
                        .lastName("Pellant")
                        .build()
                    )
                    .identity(Identity.builder()
                        .dob("1998-12-31")
                        .nino("SC 94 27 06 A")
                        .build()
                    )
                    .address(Address.builder()
                        .line1("1 Appellant Close")
                        .town("Appellant-town")
                        .county("Appellant-county")
                        .postcode("TS1 1ST")
                        .build()
                    )
                    .contact(Contact.builder()
                        .mobile("07911123456")
                        .email("appellant@gmail.com")
                        .build()
                    )
                    .isAppointee("No")
                    .build()
                )
                .mrnDetails(MrnDetails.builder()
                    .mrnDate("2010-02-01")
                    .mrnLateReason("Forgot to send it")
                    .dwpIssuingOffice("DWP PIP (1)")
                    .build()
                )
                .appealReasons(
                    AppealReasons.builder()
                        .reasons(
                            Collections.singletonList(
                                AppealReason.builder()
                                    .value(AppealReasonDetails.builder()
                                        .reason("Underpayment")
                                        .description("I think I should get more")
                                        .build()
                                    )
                                    .build()
                            )
                        )
                        .otherReasons("I can't think of anything else")
                        .build()
                )
                .hearingOptions(null)
                .rep(Representative.builder()
                    .hasRepresentative("Yes")
                    .name(Name.builder().title("Miss.").firstName("Re").lastName("Presentative").build())
                    .contact(Contact.builder().mobile("07333333333").email("rep@gmail.com").build())
                    .address(Address.builder()
                        .line1("1 Rep Cres")
                        .town("Rep-town")
                        .county("Rep-county")
                        .postcode("TS3 3ST")
                        .build())
                    .build())
                .build()
            )
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(Subscription.builder()
                    .subscribeEmail("Yes")
                    .email("appellant@gmail.com")
                    .subscribeSms("Yes")
                    .mobile("07911123456")
                    .build()
                )
                .build()
            )
            .sscsDocument(Collections.emptyList())
            .evidencePresent("no")
            .build();

        SessionDraft actual = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertEquals("Personal Independence Payment (PIP)", actual.getBenefitType().getBenefitType());
        assertEquals("yes", actual.getCreateAccount().getCreateAccount());
        assertEquals("yes", actual.getHaveAMrn().getHaveAMrn());
        assertEquals("1", actual.getMrnDate().getMrnDateDetails().getDay());
        assertEquals("2", actual.getMrnDate().getMrnDateDetails().getMonth());
        assertEquals("2010", actual.getMrnDate().getMrnDateDetails().getYear());
        assertEquals("1", actual.getDwpIssuingOffice().getPipNumber());
        assertEquals("no", actual.getAppointee().getIsAppointee());
        assertEquals("Mrs.", actual.getAppellantName().getTitle());
        assertEquals("Ap", actual.getAppellantName().getFirstName());
        assertEquals("Pellant", actual.getAppellantName().getLastName());
        assertEquals("31", actual.getAppellantDob().getDate().getDay());
        assertEquals("12", actual.getAppellantDob().getDate().getMonth());
        assertEquals("1998", actual.getAppellantDob().getDate().getYear());
        assertEquals("SC 94 27 06 A", actual.getAppellantNino().getNino());
        assertEquals("1 Appellant Close", actual.getAppellantContactDetails().getAddressLine1());
        assertNull(actual.getAppellantContactDetails().getAddressLine2());
        assertEquals("Appellant-town", actual.getAppellantContactDetails().getTownCity());
        assertEquals("Appellant-county", actual.getAppellantContactDetails().getCounty());
        assertEquals("TS1 1ST", actual.getAppellantContactDetails().getPostCode());
        assertEquals("07911123456", actual.getAppellantContactDetails().getPhoneNumber());
        assertEquals("appellant@gmail.com", actual.getAppellantContactDetails().getEmailAddress());
        assertEquals("yes", actual.getTextReminders().getDoYouWantTextMsgReminders());
        assertEquals("yes", actual.getSendToNumber().getUseSameNumber());
        assertEquals("yes", actual.getRepresentative().getHasRepresentative());
        assertEquals("Miss.", actual.getRepresentativeDetails().getName().getTitle());
        assertEquals("Re", actual.getRepresentativeDetails().getName().getFirst());
        assertEquals("Presentative", actual.getRepresentativeDetails().getName().getLast());
        assertEquals("I think I should get more", actual.getReasonForAppealing().getReasonForAppealingItems().get(0).getReasonForAppealing());
        assertEquals("Underpayment", actual.getReasonForAppealing().getReasonForAppealingItems().get(0).getWhatYouDisagreeWith());
        assertEquals("I can't think of anything else", actual.getOtherReasonForAppealing().getOtherReasonForAppealing());
        assertEquals("no", actual.getEvidenceProvide().getEvidenceProvide());
        assertEquals("Miss.", actual.getRepresentativeDetails().getName().getTitle());
        assertEquals("Re", actual.getRepresentativeDetails().getName().getFirst());
        assertEquals("Presentative", actual.getRepresentativeDetails().getName().getLast());
        assertEquals("07333333333", actual.getRepresentativeDetails().getPhoneNumber());
        assertEquals("rep@gmail.com", actual.getRepresentativeDetails().getEmailAddress());
        assertEquals("1 Rep Cres", actual.getRepresentativeDetails().getAddressLine1());
        assertNull(actual.getRepresentativeDetails().getAddressLine2());
        assertEquals("Rep-town", actual.getRepresentativeDetails().getTownCity());
        assertEquals("Rep-county", actual.getRepresentativeDetails().getCounty());
        assertEquals("TS3 3ST", actual.getRepresentativeDetails().getPostCode());
    }

    @Test
    public void convertPopulatedCaseDataWithRepNoAddress() {
        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("PIP")
                    .description("Personal Independence Payment")
                    .build()
                )
                .appellant(Appellant.builder()
                    .name(Name.builder()
                        .title("Mrs.")
                        .firstName("Ap")
                        .lastName("Pellant")
                        .build()
                    )
                    .identity(Identity.builder()
                        .dob("1998-12-31")
                        .nino("SC 94 27 06 A")
                        .build()
                    )
                    .address(Address.builder()
                        .line1("1 Appellant Close")
                        .town("Appellant-town")
                        .county("Appellant-county")
                        .postcode("TS1 1ST")
                        .build()
                    )
                    .contact(Contact.builder()
                        .mobile("07911123456")
                        .email("appellant@gmail.com")
                        .build()
                    )
                    .isAppointee("No")
                    .build()
                )
                .mrnDetails(MrnDetails.builder()
                    .mrnDate("2010-02-01")
                    .mrnLateReason("Forgot to send it")
                    .dwpIssuingOffice("DWP PIP (1)")
                    .build()
                )
                .appealReasons(
                    AppealReasons.builder()
                        .reasons(
                            Collections.singletonList(
                                AppealReason.builder()
                                    .value(AppealReasonDetails.builder()
                                        .reason("Underpayment")
                                        .description("I think I should get more")
                                        .build()
                                    )
                                    .build()
                            )
                        )
                        .otherReasons("I can't think of anything else")
                        .build()
                )
                .hearingOptions(null)
                .rep(Representative.builder()
                    .hasRepresentative("Yes")
                    .name(Name.builder().title("Miss.").firstName("Re").lastName("Presentative").build())
                    .contact(Contact.builder().mobile("07333333333").email("rep@gmail.com").build())
                    .address(null)
                    .build())
                .build()
            )
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(Subscription.builder()
                    .subscribeEmail("Yes")
                    .email("appellant@gmail.com")
                    .subscribeSms("Yes")
                    .mobile("07911123456")
                    .build()
                )
                .build()
            )
            .sscsDocument(Collections.emptyList())
            .evidencePresent("no")
            .build();

        SessionDraft actual = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertNull(actual.getRepresentativeDetails().getAddressLine1());
        assertNull(actual.getRepresentativeDetails().getAddressLine2());
        assertNull(actual.getRepresentativeDetails().getTownCity());
        assertNull(actual.getRepresentativeDetails().getCounty());
        assertNull(actual.getRepresentativeDetails().getPostCode());
    }

    @Test
    public void convertPopulatedCaseDataWithRepNoContact() {
        SscsCaseData caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("PIP")
                    .description("Personal Independence Payment")
                    .build()
                )
                .appellant(Appellant.builder()
                    .name(Name.builder()
                        .title("Mrs.")
                        .firstName("Ap")
                        .lastName("Pellant")
                        .build()
                    )
                    .identity(Identity.builder()
                        .dob("1998-12-31")
                        .nino("SC 94 27 06 A")
                        .build()
                    )
                    .address(Address.builder()
                        .line1("1 Appellant Close")
                        .town("Appellant-town")
                        .county("Appellant-county")
                        .postcode("TS1 1ST")
                        .build()
                    )
                    .contact(Contact.builder()
                        .mobile("07911123456")
                        .email("appellant@gmail.com")
                        .build()
                    )
                    .isAppointee("No")
                    .build()
                )
                .mrnDetails(MrnDetails.builder()
                    .mrnDate("2010-02-01")
                    .mrnLateReason("Forgot to send it")
                    .dwpIssuingOffice("DWP PIP (1)")
                    .build()
                )
                .appealReasons(
                    AppealReasons.builder()
                        .reasons(
                            Collections.singletonList(
                                AppealReason.builder()
                                    .value(AppealReasonDetails.builder()
                                        .reason("Underpayment")
                                        .description("I think I should get more")
                                        .build()
                                    )
                                    .build()
                            )
                        )
                        .otherReasons("I can't think of anything else")
                        .build()
                )
                .hearingOptions(null)
                .rep(Representative.builder()
                    .hasRepresentative("Yes")
                    .name(Name.builder().title("Miss.").firstName("Re").lastName("Presentative").build())
                    .contact(null)
                    .address(Address.builder()
                        .line1("1 Rep Cres")
                        .town("Rep-town")
                        .county("Rep-county")
                        .postcode("TS3 3ST")
                        .build())
                    .build())
                .build()
            )
            .subscriptions(Subscriptions.builder()
                .appellantSubscription(Subscription.builder()
                    .subscribeEmail("Yes")
                    .email("appellant@gmail.com")
                    .subscribeSms("Yes")
                    .mobile("07911123456")
                    .build()
                )
                .build()
            )
            .sscsDocument(Collections.emptyList())
            .evidencePresent("no")
            .build();

        SessionDraft actual = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertNull(actual.getRepresentativeDetails().getPhoneNumber());
        assertNull(actual.getRepresentativeDetails().getEmailAddress());
    }

    @Test
    public void convertPopulatedCaseDataWhenAttendingHearing() {
        caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("PIP")
                    .description("Personal Independence Payment")
                    .build()
                )
                .appellant(Appellant.builder()
                    .name(Name.builder()
                        .title("Mrs.")
                        .firstName("Ap")
                        .lastName("Pellant")
                        .build()
                    )
                    .identity(Identity.builder()
                        .dob("1998-12-31")
                        .nino("SC 94 27 06 A")
                        .build()
                    )
                    .address(Address.builder()
                        .line1("1 Appellant Close")
                        .town("Appellant-town")
                        .county("Appellant-county")
                        .postcode("TS1 1ST")
                        .build()
                    )
                    .contact(Contact.builder()
                        .mobile("07911123456")
                        .email("appellant@gmail.com")
                        .build()
                    )
                    .isAppointee("No")
                    .build()
                )
                .mrnDetails(MrnDetails.builder()
                    .mrnDate("2010-02-01")
                    .mrnLateReason("Forgot to send it")
                    .dwpIssuingOffice("DWP PIP (1)")
                    .build()
                )
                .appealReasons(
                    AppealReasons.builder()
                        .reasons(
                            Collections.singletonList(
                                AppealReason.builder()
                                    .value(AppealReasonDetails.builder()
                                        .reason("Underpayment")
                                        .description("I think I should get more")
                                        .build()
                                    )
                                    .build()
                            )
                        )
                        .otherReasons("I can't think of anything else")
                        .build()
                )
                .hearingOptions(
                    HearingOptions.builder()
                        .wantsToAttend("Yes")
                        .scheduleHearing("Yes")
                        .wantsSupport("No")
                        .build()
                )
                .hearingSubtype(new HearingSubtype("Yes", "999", null, null, "No"))
                .build()
            )
            .evidencePresent("no")
            .build();

        SessionDraft actual = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertEquals("yes", actual.getTheHearing().getAttendHearing());
        assertEquals("yes", actual.getHearingAvailability().getScheduleHearing());
        assertEquals("no", actual.getHearingSupport().getArrangements());
        assertEquals(Boolean.valueOf("true"), actual.getHearingOptions().getSelectOptions().getTelephone().getRequested());
        assertEquals(Boolean.valueOf("false"), actual.getHearingOptions().getSelectOptions().getVideo().getRequested());
        assertEquals(Boolean.valueOf("false"), actual.getHearingOptions().getSelectOptions().getFaceToFace().getRequested());
        assertEquals("999", actual.getHearingOptions().getSelectOptions().getTelephone().getPhoneNumber());
    }

    @Test
    public void convertPopulatedCaseDataWhenAttendingHearingWithSupport() {
        caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("PIP")
                    .description("Personal Independence Payment")
                    .build()
                )
                .appellant(Appellant.builder()
                    .name(Name.builder()
                        .title("Mrs.")
                        .firstName("Ap")
                        .lastName("Pellant")
                        .build()
                    )
                    .identity(Identity.builder()
                        .dob("1998-12-31")
                        .nino("SC 94 27 06 A")
                        .build()
                    )
                    .address(Address.builder()
                        .line1("1 Appellant Close")
                        .town("Appellant-town")
                        .county("Appellant-county")
                        .postcode("TS1 1ST")
                        .build()
                    )
                    .contact(Contact.builder()
                        .mobile("07911123456")
                        .email("appellant@gmail.com")
                        .build()
                    )
                    .isAppointee("No")
                    .build()
                )
                .mrnDetails(MrnDetails.builder()
                    .mrnDate("2010-02-01")
                    .mrnLateReason("Forgot to send it")
                    .dwpIssuingOffice("DWP PIP (1)")
                    .build()
                )
                .appealReasons(
                    AppealReasons.builder()
                        .reasons(
                            Collections.singletonList(
                                AppealReason.builder()
                                    .value(AppealReasonDetails.builder()
                                        .reason("Underpayment")
                                        .description("I think I should get more")
                                        .build()
                                    )
                                    .build()
                            )
                        )
                        .otherReasons("I can't think of anything else")
                        .build()
                )
                .hearingOptions(
                    HearingOptions.builder()
                        .wantsToAttend("Yes")
                        .scheduleHearing("Yes")
                        .wantsSupport("Yes")
                        .languageInterpreter("Yes")
                        .languages("Spanish")
                        .signLanguageType("British Sign Language (BSL)")
                        .other("Help with stairs")
                        .arrangements(
                            Arrays.asList(
                                "hearingLoop",
                                "accessibleHearingRoom",
                                "signLanguageInterpreter",
                                "disabledAccess"
                            )
                        )
                        .build()
                )
                .build()
            )
            .evidencePresent("no")
            .build();

        SessionDraft actual = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertEquals("yes", actual.getTheHearing().getAttendHearing());
        assertEquals("yes", actual.getHearingAvailability().getScheduleHearing());
        assertEquals("yes", actual.getHearingSupport().getArrangements());
        assertTrue(actual.getHearingArrangements().getSelection().getInterpreterLanguage().getRequested());
        assertEquals(
            "Spanish",
            actual.getHearingArrangements().getSelection().getInterpreterLanguage().getLanguage()
        );
        assertTrue(actual.getHearingArrangements().getSelection().getSignLanguage().getRequested());
        assertEquals(
            "British Sign Language (BSL)",
            actual.getHearingArrangements().getSelection().getSignLanguage().getLanguage()
        );
        assertTrue(actual.getHearingArrangements().getSelection().getHearingLoop().getRequested());
        assertTrue(actual.getHearingArrangements().getSelection().getAccessibleHearingRoom().getRequested());
        assertTrue(actual.getHearingArrangements().getSelection().getAnythingElse().getRequested());
        assertEquals(
            "Help with stairs",
            actual.getHearingArrangements().getSelection().getAnythingElse().getLanguage()
        );
    }

    @Test
    public void convertPopulatedCaseDataWhenAttendingHearingWithSupportNoInterpreter() {
        caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("PIP")
                    .description("Personal Independence Payment")
                    .build()
                )
                .appellant(Appellant.builder()
                    .name(Name.builder()
                        .title("Mrs.")
                        .firstName("Ap")
                        .lastName("Pellant")
                        .build()
                    )
                    .identity(Identity.builder()
                        .dob("1998-12-31")
                        .nino("SC 94 27 06 A")
                        .build()
                    )
                    .address(Address.builder()
                        .line1("1 Appellant Close")
                        .town("Appellant-town")
                        .county("Appellant-county")
                        .postcode("TS1 1ST")
                        .build()
                    )
                    .contact(Contact.builder()
                        .mobile("07911123456")
                        .email("appellant@gmail.com")
                        .build()
                    )
                    .isAppointee("No")
                    .build()
                )
                .mrnDetails(MrnDetails.builder()
                    .mrnDate("2010-02-01")
                    .mrnLateReason("Forgot to send it")
                    .dwpIssuingOffice("DWP PIP (1)")
                    .build()
                )
                .appealReasons(
                    AppealReasons.builder()
                        .reasons(
                            Collections.singletonList(
                                AppealReason.builder()
                                    .value(AppealReasonDetails.builder()
                                        .reason("Underpayment")
                                        .description("I think I should get more")
                                        .build()
                                    )
                                    .build()
                            )
                        )
                        .otherReasons("I can't think of anything else")
                        .build()
                )
                .hearingOptions(
                    HearingOptions.builder()
                        .wantsToAttend("Yes")
                        .scheduleHearing("Yes")
                        .wantsSupport("Yes")
                        .languageInterpreter("No")
                        .signLanguageType("British Sign Language (BSL)")
                        .other("Help with stairs")
                        .arrangements(Arrays.asList(
                            HearingOptionArrangements.SIGN_LANGUAGE_INTERPRETER.getValue(),
                            HearingOptionArrangements.HEARING_LOOP.getValue(),
                            HearingOptionArrangements.DISABLE_ACCESS.getValue()))
                        .build()
                )
                .build()
            )
            .evidencePresent("no")
            .build();

        SessionDraft actual = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertEquals("yes", actual.getTheHearing().getAttendHearing());
        assertEquals("yes", actual.getHearingAvailability().getScheduleHearing());
        assertEquals("yes", actual.getHearingSupport().getArrangements());
        assertFalse(actual.getHearingArrangements().getSelection().getInterpreterLanguage().getRequested());
        assertNull(actual.getHearingArrangements().getSelection().getInterpreterLanguage().getLanguage());
        assertTrue(actual.getHearingArrangements().getSelection().getSignLanguage().getRequested());
        assertEquals(
            "British Sign Language (BSL)",
            actual.getHearingArrangements().getSelection().getSignLanguage().getLanguage()
        );
        assertTrue(actual.getHearingArrangements().getSelection().getHearingLoop().getRequested());
        assertTrue(actual.getHearingArrangements().getSelection().getAccessibleHearingRoom().getRequested());
        assertTrue(actual.getHearingArrangements().getSelection().getAnythingElse().getRequested());
        assertEquals(
            "Help with stairs",
            actual.getHearingArrangements().getSelection().getAnythingElse().getLanguage()
        );
    }

    @Test
    public void convertPopulatedCaseDataWhenAttendingHearingWithSupportNoArrangements() {
        caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .hearingOptions(
                    HearingOptions.builder()
                        .wantsToAttend("Yes")
                        .scheduleHearing("Yes")
                        .wantsSupport("Yes")
                        .languageInterpreter("No")
                        .signLanguageType("British Sign Language (BSL)")
                        .other("Help with stairs")
                        .arrangements(Collections.emptyList())
                        .build())
                .build())
            .evidencePresent("no")
            .build();

        SessionDraft actual = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertEquals("yes", actual.getTheHearing().getAttendHearing());
        assertEquals("yes", actual.getHearingAvailability().getScheduleHearing());
        assertEquals("yes", actual.getHearingSupport().getArrangements());
        assertFalse(actual.getHearingArrangements().getSelection().getInterpreterLanguage().getRequested());
        assertNull(actual.getHearingArrangements().getSelection().getInterpreterLanguage().getLanguage());
        assertFalse(actual.getHearingArrangements().getSelection().getSignLanguage().getRequested());
        assertFalse(actual.getHearingArrangements().getSelection().getHearingLoop().getRequested());
        assertFalse(actual.getHearingArrangements().getSelection().getAccessibleHearingRoom().getRequested());
    }

    @Test
    public void convertPopulatedCaseDataWhenAttendingHearingWithSupportNullObjectsArrangements() {
        caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .hearingOptions(
                    HearingOptions.builder()
                        .wantsToAttend("Yes")
                        .scheduleHearing("Yes")
                        .wantsSupport("Yes")
                        .languageInterpreter(null)
                        .signLanguageType(null)
                        .other("Help with stairs")
                        .arrangements(null)
                        .build()
                )
                .build()
            )
            .evidencePresent("no")
            .build();

        SessionDraft actual = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertEquals("yes", actual.getTheHearing().getAttendHearing());
        assertEquals("yes", actual.getHearingAvailability().getScheduleHearing());
        assertEquals("yes", actual.getHearingSupport().getArrangements());
        assertNull(actual.getHearingArrangements());
    }

    @Test
    @Parameters(method = "getHearingOptionsScenarios")
    public void givenHearingWithSupportAndNoArrangement_shouldReturnNullSessionHearingArrangements(
        HearingOptions hearingOptions, SessionHearingArrangements expected) {

        caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .hearingOptions(hearingOptions)
                .build())
            .build();

        SessionDraft actual = convertSscsCaseDataIntoSessionDraft.convert(caseData);

        assertEquals(expected, actual.getHearingArrangements());
    }

    @SuppressWarnings("unused")
    private Object[] getHearingOptionsScenarios() {
        HearingOptions hearingOptionsWithNullArrangement = HearingOptions.builder()
            .wantsToAttend("Yes")
            .wantsSupport("Yes")
            .build();

        HearingOptions hearingOptionsWithEmptyArrangement = HearingOptions.builder()
            .wantsToAttend("Yes")
            .wantsSupport("Yes")
            .arrangements(Collections.emptyList())
            .build();

        HearingOptions hearingOptionsWithNoValidArrangement = HearingOptions.builder()
            .wantsToAttend("Yes")
            .wantsSupport("Yes")
            .arrangements(Collections.singletonList("any invalid arrangement"))
            .build();

        HearingOptions hearingOptionsWithValidArrangement = HearingOptions.builder()
            .wantsToAttend("Yes")
            .wantsSupport("Yes")
            .arrangements(Arrays.asList(
                HearingOptionArrangements.HEARING_LOOP.getValue(),
                HearingOptionArrangements.DISABLE_ACCESS.getValue()))
            .build();

        SessionHearingArrangements expected = new SessionHearingArrangements(
            new SessionHearingArrangementsSelection(
                null,
                null,
                new SessionHearingArrangement(true, null),
                new SessionHearingArrangement(true, null),
                null)
        );

        return new Object[]{
            new Object[]{hearingOptionsWithNullArrangement, null},
            new Object[]{hearingOptionsWithEmptyArrangement, null},
            new Object[]{hearingOptionsWithNoValidArrangement, null},
            new Object[]{hearingOptionsWithValidArrangement, expected}
        };
    }


    @Test
    public void convertPopulatedCaseDataWhenAttendingHearingWithSupportNotListedArrangements() {
        caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .hearingOptions(
                    HearingOptions.builder()
                        .wantsToAttend("Yes")
                        .scheduleHearing("Yes")
                        .wantsSupport("Yes")
                        .languageInterpreter(null)
                        .signLanguageType(null)
                        .other("Help with stairs")
                        .arrangements(Collections.singletonList(""))
                        .build()
                )
                .build()
            )
            .evidencePresent("no")
            .build();

        SessionDraft actual = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertEquals("yes", actual.getTheHearing().getAttendHearing());
        assertEquals("yes", actual.getHearingAvailability().getScheduleHearing());
        assertEquals("yes", actual.getHearingSupport().getArrangements());
        assertNull(actual.getHearingArrangements());
    }

    @Test
    public void convertPopulatedCaseDataWhenAttendingHearingWithSupportNullArrangements() {
        caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .hearingOptions(
                    HearingOptions.builder()
                        .wantsToAttend("Yes")
                        .scheduleHearing("Yes")
                        .wantsSupport("Yes")
                        .languageInterpreter("No")
                        .signLanguageType("British Sign Language (BSL)")
                        .other("Help with stairs")
                        .arrangements(null)
                        .build()
                )
                .build()
            )
            .evidencePresent("no")
            .build();

        SessionDraft actual = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertEquals("yes", actual.getTheHearing().getAttendHearing());
        assertEquals("yes", actual.getHearingAvailability().getScheduleHearing());
        assertEquals("yes", actual.getHearingSupport().getArrangements());
        assertFalse(actual.getHearingArrangements().getSelection().getInterpreterLanguage().getRequested());
        assertNull(actual.getHearingArrangements().getSelection().getInterpreterLanguage().getLanguage());
        assertFalse(actual.getHearingArrangements().getSelection().getSignLanguage().getRequested());
        assertFalse(actual.getHearingArrangements().getSelection().getHearingLoop().getRequested());
        assertFalse(actual.getHearingArrangements().getSelection().getAccessibleHearingRoom().getRequested());
    }

    @Test
    public void convertPopulatedCaseDataWhenAttendingHearingButCantAttendOnSomeDates() {
        caseData = SscsCaseData.builder()
            .appeal(Appeal.builder()
                .benefitType(BenefitType.builder()
                    .code("PIP")
                    .description("Personal Independence Payment")
                    .build()
                )
                .appellant(Appellant.builder()
                    .name(Name.builder()
                        .title("Mrs.")
                        .firstName("Ap")
                        .lastName("Pellant")
                        .build()
                    )
                    .identity(Identity.builder()
                        .dob("1998-12-31")
                        .nino("SC 94 27 06 A")
                        .build()
                    )
                    .address(Address.builder()
                        .line1("1 Appellant Close")
                        .town("Appellant-town")
                        .county("Appellant-county")
                        .postcode("TS1 1ST")
                        .build()
                    )
                    .contact(Contact.builder()
                        .mobile("07911123456")
                        .email("appellant@gmail.com")
                        .build()
                    )
                    .isAppointee("No")
                    .build()
                )
                .mrnDetails(MrnDetails.builder()
                    .mrnDate("2010-02-01")
                    .mrnLateReason("Forgot to send it")
                    .dwpIssuingOffice("DWP PIP (1)")
                    .build()
                )
                .appealReasons(
                    AppealReasons.builder()
                        .reasons(
                            Collections.singletonList(
                                AppealReason.builder()
                                    .value(AppealReasonDetails.builder()
                                        .reason("Underpayment")
                                        .description("I think I should get more")
                                        .build()
                                    )
                                    .build()
                            )
                        )
                        .otherReasons("I can't think of anything else")
                        .build()
                )
                .hearingOptions(
                    HearingOptions.builder()
                        .wantsToAttend("Yes")
                        .scheduleHearing("Yes")
                        .wantsSupport("No")
                        .excludeDates(
                            Arrays.asList(
                                ExcludeDate.builder()
                                    .value(
                                        DateRange.builder()
                                            .start("2099-05-01")
                                            .end("2099-05-03")
                                            .build()
                                    )
                                    .build(),
                                ExcludeDate.builder()
                                    .value(
                                        DateRange.builder()
                                            .start("2099-05-12")
                                            .end("2099-05-12")
                                            .build()
                                    )
                                    .build()
                            )
                        )
                        .build()
                )
                .build()
            )
            .evidencePresent("no")
            .build();

        SessionDraft actual = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertEquals("yes", actual.getTheHearing().getAttendHearing());
        assertEquals("yes", actual.getHearingAvailability().getScheduleHearing());
        assertEquals("no", actual.getHearingSupport().getArrangements());
        assertNotNull(actual.getDatesCantAttend());
        assertNotNull(actual.getDatesCantAttend().getDatesCantAttend());
        assertEquals(2, actual.getDatesCantAttend().getDatesCantAttend().size());
        assertEquals("1", actual.getDatesCantAttend().getDatesCantAttend().get(0).getDay());
        assertEquals("5", actual.getDatesCantAttend().getDatesCantAttend().get(0).getMonth());
        assertEquals("2099", actual.getDatesCantAttend().getDatesCantAttend().get(0).getYear());
        assertEquals("12", actual.getDatesCantAttend().getDatesCantAttend().get(1).getDay());
        assertEquals("5", actual.getDatesCantAttend().getDatesCantAttend().get(1).getMonth());
        assertEquals("2099", actual.getDatesCantAttend().getDatesCantAttend().get(1).getYear());
    }

    @Test
    public void givenCaseWithPcqId_shouldReturnResponseWithPcqId() {
        caseData = SscsCaseData.builder().appeal(Appeal.builder().build()).pcqId("12345").build();

        SessionDraft actualSessionDraft = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertEquals("12345", actualSessionDraft.getPcqId().getPcqId());
    }

    @Test
    public void convertPopulatedCaseDataWhenLanguagePreferenceWelshIsGiven() {
        caseData = SscsCaseData.builder()
                .appeal(Appeal.builder()
                        .hearingOptions(
                                HearingOptions.builder()
                                        .wantsToAttend("Yes")
                                        .scheduleHearing("Yes")
                                        .wantsSupport("Yes")
                                        .languageInterpreter(null)
                                        .signLanguageType(null)
                                        .other("Help with stairs")
                                        .arrangements(Collections.singletonList(""))
                                        .build()
                        )
                        .build()
                )
                .evidencePresent("no")
                .languagePreferenceWelsh("yes")
                .build();
        SessionDraft actual = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertEquals("yes", actual.getLanguagePreferenceWelsh().getLanguagePreferenceWelsh());
    }

    @Test
    public void convertPopulatedCaseDataWhenLanguagePreferenceWelshIsNotGiven() {
        caseData = SscsCaseData.builder()
                .appeal(Appeal.builder()
                        .hearingOptions(
                                HearingOptions.builder()
                                        .wantsToAttend("Yes")
                                        .scheduleHearing("Yes")
                                        .wantsSupport("Yes")
                                        .languageInterpreter(null)
                                        .signLanguageType(null)
                                        .other("Help with stairs")
                                        .arrangements(Collections.singletonList(""))
                                        .build()
                        )
                        .build()
                )
                .evidencePresent("no")
                .languagePreferenceWelsh(null)
                .build();
        SessionDraft actual = convertSscsCaseDataIntoSessionDraft.convert(caseData);
        assertNull(actual.getLanguagePreferenceWelsh());
    }

}
