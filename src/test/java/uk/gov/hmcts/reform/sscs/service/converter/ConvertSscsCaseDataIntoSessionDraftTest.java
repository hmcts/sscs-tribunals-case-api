package uk.gov.hmcts.reform.sscs.service.converter;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.domain.*;
import uk.gov.hmcts.reform.sscs.model.draft.SessionDraft;

public class ConvertSscsCaseDataIntoSessionDraftTest {
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
    public void convertPopulatedCaseData() {
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
                .hearingOptions(
                    HearingOptions.builder()
                        .wantsToAttend("yes")
                        .wantsSupport("yes")
                        .languageInterpreter("Spanish")
                        .signLanguageType("British Sign Language (BSL)")
                        .build()
                )
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
            .evidencePresent("no")
            .build();

        SessionDraft actual = new ConvertSscsCaseDataIntoSessionDraft().convert(caseData);
        assertEquals("Personal Independence Payment (PIP)", actual.getBenefitType().getBenefitType());
        assertEquals("yes", actual.getCreateAccount().getCreateAccount());
        assertEquals("yes", actual.getHaveAMrn().getHaveAMrn());
        assertEquals("1", actual.getMrnDate().getMrnDateDetails().getDay());
        assertEquals("2", actual.getMrnDate().getMrnDateDetails().getMonth());
        assertEquals("2010", actual.getMrnDate().getMrnDateDetails().getYear());
        assertEquals("yes", actual.getCheckMrn().getCheckedMrn());
        assertEquals("Forgot to send it", actual.getMrnOverThirteenMonthsLate().getReasonForBeingLate());
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
        assertEquals(null, actual.getAppellantContactDetails().getAddressLine2());
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
        assertEquals("yes", actual.getTheHearing().getAttendHearing());
        assertEquals("yes", actual.getHearingSupport().getArrangements());
        assertNull(actual.getRepresentativeDetails());
    }

    @Test
    public void convertPopulatedCaseDataWithEvidence() {
        // TODO
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
                .rep(Representative.builder()
                    .hasRepresentative("Yes")
                    .organisation("Representative Inc")
                    .name(Name.builder()
                        .title("Mr.")
                        .firstName("Re")
                        .lastName("Presentative")
                        .build()
                    )
                    .contact(Contact.builder()
                        .mobile("07222222222")
                        .email("representative@test.com")
                        .build()
                    )
                    .address(Address.builder()
                        .line1("rep-line1")
                        .town("rep-town")
                        .county("rep-county")
                        .postcode("TS3 3ST")
                        .build()
                    )
                    .build()
                )
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
            .build();

        SessionDraft actual = new ConvertSscsCaseDataIntoSessionDraft().convert(caseData);
        assertEquals("Personal Independence Payment (PIP)", actual.getBenefitType().getBenefitType());
        assertEquals("yes", actual.getCreateAccount().getCreateAccount());
        assertEquals("yes", actual.getHaveAMrn().getHaveAMrn());
        assertEquals("1", actual.getMrnDate().getMrnDateDetails().getDay());
        assertEquals("2", actual.getMrnDate().getMrnDateDetails().getMonth());
        assertEquals("2010", actual.getMrnDate().getMrnDateDetails().getYear());
        assertEquals("yes", actual.getCheckMrn().getCheckedMrn());
        assertEquals("Forgot to send it", actual.getMrnOverThirteenMonthsLate().getReasonForBeingLate());
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
        assertEquals(null, actual.getAppellantContactDetails().getAddressLine2());
        assertEquals("Appellant-town", actual.getAppellantContactDetails().getTownCity());
        assertEquals("Appellant-county", actual.getAppellantContactDetails().getCounty());
        assertEquals("TS1 1ST", actual.getAppellantContactDetails().getPostCode());
        assertEquals("07911123456", actual.getAppellantContactDetails().getPhoneNumber());
        assertEquals("appellant@gmail.com", actual.getAppellantContactDetails().getEmailAddress());
        assertEquals("yes", actual.getTextReminders().getDoYouWantTextMsgReminders());
        assertEquals("yes", actual.getSendToNumber().getUseSameNumber());
        assertEquals("yes", actual.getRepresentative().getHasRepresentative());
        assertEquals("Mr.", actual.getRepresentativeDetails().getName().getTitle());
        assertEquals("Re", actual.getRepresentativeDetails().getName().getFirst());
        assertEquals("Presentative", actual.getRepresentativeDetails().getName().getLast());
        assertEquals("rep-line1", actual.getRepresentativeDetails().getAddressLine1());
        assertEquals(null, actual.getRepresentativeDetails().getAddressLine2());
        assertEquals("rep-town", actual.getRepresentativeDetails().getTownCity());
        assertEquals("rep-county", actual.getRepresentativeDetails().getCounty());
        assertEquals("TS3 3ST", actual.getRepresentativeDetails().getPostCode());
        assertEquals("07222222222", actual.getRepresentativeDetails().getPhoneNumber());
        assertEquals("representative@test.com", actual.getRepresentativeDetails().getEmailAddress());
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
                .build()
            )
            .build();

        SessionDraft actual = new ConvertSscsCaseDataIntoSessionDraft().convert(caseData);
        assertEquals("yes", actual.getAppointee().getIsAppointee());
    }

    @Test
    public void convertPopulatedCaseDataWithoutMrnDate() {
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
                    .build()
                )
                .mrnDetails(MrnDetails.builder()
                    .build()
                )
                .build()
            )
            .build();

        SessionDraft actual = new ConvertSscsCaseDataIntoSessionDraft().convert(caseData);
        assertNull(actual.getHaveAMrn());
        assertNull(actual.getMrnDate());
        assertEquals("no", actual.getCheckMrn().getCheckedMrn());
        assertNull(actual.getMrnOverThirteenMonthsLate());
    }
}
