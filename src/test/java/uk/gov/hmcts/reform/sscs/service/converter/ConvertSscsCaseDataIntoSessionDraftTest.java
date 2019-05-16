package uk.gov.hmcts.reform.sscs.service.converter;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.draft.*;

@RunWith(JUnitParamsRunner.class)
public class ConvertSscsCaseDataIntoSessionDraftTest {

    private SscsCaseData caseData;

    @Test(expected = NullPointerException.class)
    public void attemptToConvertNull() {
        new ConvertSscsCaseDataIntoSessionDraft().convert(null);
    }

    @Test(expected = NullPointerException.class)
    public void attemptToConvertNullAppeal() {
        SscsCaseData caseData = SscsCaseData.builder().build();
        new ConvertSscsCaseDataIntoSessionDraft().convert(caseData);
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

        SessionDraft actualSessionDraft = new ConvertSscsCaseDataIntoSessionDraft().convert(caseData);

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

        SessionDraft actualSessionDraft = new ConvertSscsCaseDataIntoSessionDraft().convert(caseData);
        assertEquals(actualSessionDraft.getMrnDate(), expectedSessionDraft.getMrnDate());
        assertEquals(actualSessionDraft.getNoMrn(), expectedSessionDraft.getNoMrn());
        assertEquals(actualSessionDraft.getHaveAMrn(), expectedSessionDraft.getHaveAMrn());
        assertEquals(actualSessionDraft.getHaveContactedDwp(), expectedSessionDraft.getHaveContactedDwp());

        System.out.println(actualSessionDraft);

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
            .build();

        SessionDraft actual = new ConvertSscsCaseDataIntoSessionDraft().convert(caseData);
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
        assertNull("no", actual.getRepresentative());
        assertEquals("I think I should get more", actual.getReasonForAppealing().getReasonForAppealingItems().get(0).getReasonForAppealing());
        assertEquals("Underpayment", actual.getReasonForAppealing().getReasonForAppealingItems().get(0).getWhatYouDisagreeWith());
        assertEquals("I can't think of anything else", actual.getOtherReasonForAppealing().getOtherReasonForAppealing());
        assertEquals("no", actual.getEvidenceProvide().getEvidenceProvide());
        assertNull(actual.getRepresentativeDetails());
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

        SessionDraft actual = new ConvertSscsCaseDataIntoSessionDraft().convert(caseData);
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
                        .name(Name.builder().firstName("Ap").lastName("Pointee").build())
                        .build()
                    )
                    .isAppointee("Yes")
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

        SessionDraft actual = new ConvertSscsCaseDataIntoSessionDraft().convert(caseData);
        assertEquals("yes", actual.getAppointee().getIsAppointee());
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

        SessionDraft actual = new ConvertSscsCaseDataIntoSessionDraft().convert(caseData);
        assertNull(actual.getAppointee());
    }

    @Test
    public void convertPopulatedCaseDataWithRep() {
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

        SessionDraft actual = new ConvertSscsCaseDataIntoSessionDraft().convert(caseData);
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
                .build()
            )
            .evidencePresent("no")
            .build();

        SessionDraft actual = new ConvertSscsCaseDataIntoSessionDraft().convert(caseData);
        assertEquals("yes", actual.getTheHearing().getAttendHearing());
        assertEquals("yes", actual.getHearingAvailability().getScheduleHearing());
        assertEquals("no", actual.getHearingSupport().getArrangements());
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

        SessionDraft actual = new ConvertSscsCaseDataIntoSessionDraft().convert(caseData);
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

    @Ignore
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
                        .build()
                )
                .build()
            )
            .evidencePresent("no")
            .build();

        SessionDraft actual = new ConvertSscsCaseDataIntoSessionDraft().convert(caseData);
        assertEquals("yes", actual.getTheHearing().getAttendHearing());
        assertEquals("yes", actual.getHearingAvailability().getScheduleHearing());
        assertEquals("no", actual.getHearingSupport().getArrangements());
        assertNotNull(actual.getDatesCantAttend());
        assertNotNull(actual.getDatesCantAttend().getDatesCantAttend());
        assertEquals(2, actual.getDatesCantAttend().getDatesCantAttend().size());
    }
}
