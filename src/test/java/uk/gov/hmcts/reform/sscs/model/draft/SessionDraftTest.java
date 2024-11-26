package uk.gov.hmcts.reform.sscs.model.draft;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static uk.gov.hmcts.reform.sscs.util.SerializeJsonMessageManager.SESSION_SAMPLE;
import static uk.gov.hmcts.reform.sscs.util.SerializeJsonMessageManager.SESSION_SAMPLE_ESA;
import static uk.gov.hmcts.reform.sscs.util.SerializeJsonMessageManager.SESSION_SAMPLE_IBC;
import static uk.gov.hmcts.reform.sscs.util.SerializeJsonMessageManager.SESSION_SAMPLE_IBC_INTERNATIONAL;
import static uk.gov.hmcts.reform.sscs.util.SerializeJsonMessageManager.SESSION_SAMPLE_IBC_REP;
import static uk.gov.hmcts.reform.sscs.util.SerializeJsonMessageManager.SESSION_SAMPLE_IBC_REP_INTERNATIONAL;
import static uk.gov.hmcts.reform.sscs.util.SerializeJsonMessageManager.SESSION_SAMPLE_WITH_APPOINTEE;
import static uk.gov.hmcts.reform.sscs.util.SerializeJsonMessageManager.SESSION_SAMPLE_WITH_APPOINTEE_AT_SAME_ADDRESS;
import static uk.gov.hmcts.reform.sscs.util.SerializeJsonMessageManager.SESSION_SAMPLE_WITH_DATES_CANT_ATTEND;
import static uk.gov.hmcts.reform.sscs.util.SerializeJsonMessageManager.SESSION_SAMPLE_WITH_NO_MRN;
import static uk.gov.hmcts.reform.sscs.util.SerializeJsonMessageManager.SESSION_SAMPLE_WITH_REP;

import java.util.Arrays;
import java.util.Collections;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.Test;

public class SessionDraftTest {

    @Test
    public void shouldSerializeSessionDraftAsExpected() {
        SessionDraft sessionDraft = SessionDraft.builder()
            .benefitType(new SessionBenefitType("Personal Independence Payment (PIP)"))
            .postcode(new SessionPostcodeChecker("n29ed"))
            .createAccount(new SessionCreateAccount("yes"))
            .haveAMrn(new SessionHaveAMrn("yes"))
            .mrnDate(new SessionMrnDate(new SessionDate("10", "10", "1990")))
            .checkMrn(new SessionCheckMrn("yes"))
            .mrnOverThirteenMonthsLate(new SessionMrnOverThirteenMonthsLate("aassas dasdsa dasdasda das"))
            .dwpIssuingOffice(new SessionDwpIssuingOffice("1"))
            .appointee(new SessionAppointee("no"))
            .appellantName(new SessionName("Mrs.", "Ap", "Pellant"))
            .appellantDob(new SessionDob(new SessionDate("31", "12", "1998")))
            .appellantNino(new SessionAppellantNino("SC 94 27 06 A"))
            .appellantContactDetails(
                new SessionContactDetails(
                    "1 Appellant Close",
                    null,
                    "Appellant-town",
                    "Appellant-county",
                    null,
                    "TS1 1ST",
                    null,
                    "07911123456",
                    "appellant@gmail.com",
                    null,
                    null,
                    null
                )
            )
            .sameAddress(new SessionSameAddress("no"))
            .textReminders(new SessionTextReminders("yes"))
            .sendToNumber(new SessionSendToNumber("yes"))
            .representative(new SessionRepresentative("no"))
            .reasonForAppealing(
                new SessionReasonForAppealing(
                    Collections.singletonList(
                        new SessionReasonForAppealingItem(
                            "Underpayment",
                            "I think I should get more")
                    )
                )
            )
            .otherReasonForAppealing(new SessionOtherReasonForAppealing("I can't think of anything else"))
            .evidenceProvide(new SessionEvidenceProvide("no"))
            .theHearing(new SessionTheHearing("yes"))
            .hearingSupport(new SessionHearingSupport("yes"))
            .hearingAvailability(new SessionHearingAvailability("no"))
            .hearingArrangements(
                new SessionHearingArrangements(
                    new SessionHearingArrangementsSelection(
                        new SessionHearingArrangement(true, "Spanish"),
                        new SessionHearingArrangement(true, "British Sign Language (BSL)"),
                        new SessionHearingArrangement(true),
                        new SessionHearingArrangement(true),
                        new SessionHearingArrangement(true, "Help with stairs"))
                )
            )
            .build();

        assertThatJson(SESSION_SAMPLE.getSerializedMessage())
            .isEqualTo(sessionDraft);
    }

    @Test
    public void shouldSerializeSessionDraftWithNoMrnAsExpected() {
        SessionDraft sessionDraft = SessionDraft.builder()
            .benefitType(new SessionBenefitType("Personal Independence Payment (PIP)"))
            .postcode(new SessionPostcodeChecker("n29ed"))
            .createAccount(new SessionCreateAccount("yes"))
            .haveAMrn(new SessionHaveAMrn("no"))
            .haveContactedDwp(new SessionHaveContactedDwp("yes"))
            .noMrn(new SessionNoMrn("I can't find my letter."))
            .dwpIssuingOffice(new SessionDwpIssuingOffice("1"))
            .appointee(new SessionAppointee("no"))
            .appellantName(new SessionName("Mrs.", "Ap", "Pellant"))
            .appellantDob(new SessionDob(new SessionDate("31", "12", "1998")))
            .appellantNino(new SessionAppellantNino("SC 94 27 06 A"))
            .appellantContactDetails(
                new SessionContactDetails(
                    "1 Appellant Close",
                    null,
                    "Appellant-town",
                    "Appellant-county",
                    null,
                    "TS1 1ST",
                    null,
                    "07911123456",
                    "appellant@gmail.com",
                    null,
                    null,
                    null
                )
            )
            .textReminders(new SessionTextReminders("yes"))
            .sendToNumber(new SessionSendToNumber("yes"))
            .representative(new SessionRepresentative("no"))
            .reasonForAppealing(
                new SessionReasonForAppealing(
                    Collections.singletonList(
                        new SessionReasonForAppealingItem(
                            "Underpayment",
                            "I think I should get more")
                    )
                )
            )
            .otherReasonForAppealing(new SessionOtherReasonForAppealing("I can't think of anything else"))
            .evidenceProvide(new SessionEvidenceProvide("no"))
            .theHearing(new SessionTheHearing("yes"))
            .hearingSupport(new SessionHearingSupport("yes"))
            .hearingArrangements(
                new SessionHearingArrangements(
                    new SessionHearingArrangementsSelection(
                        new SessionHearingArrangement(true, "Spanish"),
                        new SessionHearingArrangement(true, "British Sign Language (BSL)"),
                        new SessionHearingArrangement(true),
                        new SessionHearingArrangement(true),
                        new SessionHearingArrangement(true, "Help with stairs"))
                )
            )
            .build();

        assertThatJson(SESSION_SAMPLE_WITH_NO_MRN.getSerializedMessage())
            .isEqualTo(sessionDraft);
    }

    @Test
    public void shouldSerializeSessionDraftWithRepAsExpected() {
        SessionDraft sessionDraft = SessionDraft.builder()
            .benefitType(new SessionBenefitType("Personal Independence Payment (PIP)"))
            .postcode(new SessionPostcodeChecker("n29ed"))
            .createAccount(new SessionCreateAccount("yes"))
            .haveAMrn(new SessionHaveAMrn("yes"))
            .mrnDate(new SessionMrnDate(new SessionDate("10", "10", "1990")))
            .checkMrn(new SessionCheckMrn("yes"))
            .mrnOverThirteenMonthsLate(new SessionMrnOverThirteenMonthsLate("aassas dasdsa dasdasda das"))
            .dwpIssuingOffice(new SessionDwpIssuingOffice("1"))
            .appointee(new SessionAppointee("no"))
            .appellantName(new SessionName("Mrs.", "Ap", "Pellant"))
            .appellantDob(new SessionDob(new SessionDate("31", "12", "1998")))
            .appellantNino(new SessionAppellantNino("SC 94 27 06 A"))
            .appellantContactDetails(
                new SessionContactDetails(
                    "1 Appellant Close",
                    null,
                    "Appellant-town",
                    "Appellant-county",
                    null,
                    "TS1 1ST",
                    null,
                    "07911123456",
                    "appellant@gmail.com",
                    null,
                    null,
                    null
                )
            )
            .textReminders(new SessionTextReminders("yes"))
            .sendToNumber(new SessionSendToNumber("yes"))
            .representative(new SessionRepresentative("yes"))
            .representativeDetails(
                new SessionRepresentativeDetails(
                    new SessionRepName(
                        "Mr.",
                        "Re",
                        "Presentative"
                    ),
                    "rep-line1",
                    "rep-line2",
                    "rep-town-city",
                    "rep-county",
                    null,
                    "RE7 7ES",
                    "07222222222",
                    "representative@gmail.com",
                    null,
                    null,
                    null
                )
            )
            .build();

        assertThatJson(SESSION_SAMPLE_WITH_REP.getSerializedMessage())
            .when(Option.IGNORING_EXTRA_FIELDS)
            .isEqualTo(sessionDraft);
    }

    @Test
    public void shouldSerializeSessionDraftWithAppointeeAsExpected() {
        SessionDraft sessionDraft = SessionDraft.builder()
            .benefitType(new SessionBenefitType("Personal Independence Payment (PIP)"))
            .postcode(new SessionPostcodeChecker("n29ed"))
            .createAccount(new SessionCreateAccount("yes"))
            .haveAMrn(new SessionHaveAMrn("yes"))
            .mrnDate(new SessionMrnDate(new SessionDate("10", "10", "1990")))
            .checkMrn(new SessionCheckMrn("yes"))
            .mrnOverThirteenMonthsLate(new SessionMrnOverThirteenMonthsLate("aassas dasdsa dasdasda das"))
            .dwpIssuingOffice(new SessionDwpIssuingOffice("1"))
            .appointee(new SessionAppointee("yes"))
            .appointeeName(new SessionName("Mr.","Ap","Pointee"))
            .appointeeDob(new SessionDob(new SessionDate("1", "1", "1999")))
            .appointeeContactDetails(
                new SessionContactDetails(
                    "1 Appointee Street",
                    "",
                    "Appointee-town",
                    "Appointee-county",
                    null,
                    "TS2 2ST",
                    null,
                    "07111111111",
                    "appointee@test.com",
                    null,
                    null,
                    null
                )
            )
            .appellantName(new SessionName("Mrs.", "Ap", "Pellant"))
            .appellantDob(new SessionDob(new SessionDate("31", "12", "1998")))
            .appellantNino(new SessionAppellantNino("SC 94 27 06 A"))
            .appellantContactDetails(
                new SessionContactDetails(
                    "1 Appellant Close",
                    null,
                    "Appellant-town",
                    "Appellant-county",
                    null,
                    "TS1 1ST",
                    null,
                    "07911123456",
                    "appellant@gmail.com",
                    null,
                    null,
                    null
                )
            )
            .sameAddress(new SessionSameAddress("no"))
            .textReminders(new SessionTextReminders("yes"))
            .sendToNumber(new SessionSendToNumber("yes"))
            .representative(new SessionRepresentative("no"))
            .reasonForAppealing(
                new SessionReasonForAppealing(
                    Collections.singletonList(
                        new SessionReasonForAppealingItem(
                            "Underpayment",
                            "I think I should get more")
                    )
                )
            )
            .otherReasonForAppealing(new SessionOtherReasonForAppealing("I can't think of anything else"))
            .evidenceProvide(new SessionEvidenceProvide("no"))
            .theHearing(new SessionTheHearing("yes"))
            .build();

        assertThatJson(SESSION_SAMPLE_WITH_APPOINTEE.getSerializedMessage())
            .when(Option.IGNORING_EXTRA_FIELDS)
            .isEqualTo(sessionDraft);

    }

    @Test
    public void shouldSerializeSessionDraftWithAppointeeAtSameAddressAsExpected() {
        SessionDraft sessionDraft = SessionDraft.builder()
            .benefitType(new SessionBenefitType("Personal Independence Payment (PIP)"))
            .postcode(new SessionPostcodeChecker("n29ed"))
            .createAccount(new SessionCreateAccount("yes"))
            .haveAMrn(new SessionHaveAMrn("yes"))
            .mrnDate(new SessionMrnDate(new SessionDate("10", "10", "1990")))
            .checkMrn(new SessionCheckMrn("yes"))
            .mrnOverThirteenMonthsLate(new SessionMrnOverThirteenMonthsLate("aassas dasdsa dasdasda das"))
            .dwpIssuingOffice(new SessionDwpIssuingOffice("1"))
            .appointee(new SessionAppointee("yes"))
            .appointeeName(new SessionName("Mr.","Ap","Pointee"))
            .appointeeDob(new SessionDob(new SessionDate("1", "1", "1999")))
            .appointeeContactDetails(
                new SessionContactDetails(
                    "1 Appointee Street",
                    "",
                    "Appointee-town",
                    "Appointee-county",
                    null,
                    "TS2 2ST",
                    null,
                    "07111111111",
                    "appointee@test.com",
                    null,
                    null,
                    null
                )
            )
            .appellantName(new SessionName("Mrs.", "Ap", "Pellant"))
            .appellantDob(new SessionDob(new SessionDate("31", "12", "1998")))
            .appellantNino(new SessionAppellantNino("SC 94 27 06 A"))
            .sameAddress(new SessionSameAddress("yes"))
            .textReminders(new SessionTextReminders("yes"))
            .sendToNumber(new SessionSendToNumber("yes"))
            .representative(new SessionRepresentative("no"))
            .reasonForAppealing(
                new SessionReasonForAppealing(
                    Collections.singletonList(
                        new SessionReasonForAppealingItem(
                            "Underpayment",
                            "I think I should get more")
                    )
                )
            )
            .otherReasonForAppealing(new SessionOtherReasonForAppealing("I can't think of anything else"))
            .evidenceProvide(new SessionEvidenceProvide("no"))
            .theHearing(new SessionTheHearing("yes"))
            .build();

        assertThatJson(SESSION_SAMPLE_WITH_APPOINTEE_AT_SAME_ADDRESS.getSerializedMessage())
            .when(Option.IGNORING_EXTRA_FIELDS)
            .isEqualTo(sessionDraft);

    }

    @Test
    public void shouldSerializeSessionEsaDraftAsExpected() {
        SessionDraft sessionDraft = SessionDraft.builder()
            .benefitType(new SessionBenefitType("Employment and Support Allowance (ESA)"))
            .postcode(new SessionPostcodeChecker("n29ed"))
            .createAccount(new SessionCreateAccount("yes"))
            .haveAMrn(new SessionHaveAMrn("yes"))
            .mrnDate(new SessionMrnDate(new SessionDate("10", "10", "1990")))
            .checkMrn(new SessionCheckMrn("yes"))
            .mrnOverThirteenMonthsLate(new SessionMrnOverThirteenMonthsLate("aassas dasdsa dasdasda das"))
            .dwpIssuingOfficeEsa(new SessionDwpIssuingOfficeEsa("Chesterfield DRT"))
            .appointee(new SessionAppointee("no"))
            .appellantName(new SessionName("Mrs.", "Ap", "Pellant"))
            .appellantDob(new SessionDob(new SessionDate("31", "12", "1998")))
            .appellantNino(new SessionAppellantNino("SC 94 27 06 A"))
            .appellantContactDetails(
                new SessionContactDetails(
                    "1 Appellant Close",
                    null,
                    "Appellant-town",
                    "Appellant-county",
                    null,
                    "TS1 1ST",
                    null,
                    "07911123456",
                    "appellant@gmail.com",
                    null,
                    null,
                    null
                )
            )
            .textReminders(new SessionTextReminders("yes"))
            .sendToNumber(new SessionSendToNumber("yes"))
            .representative(new SessionRepresentative("no"))
            .reasonForAppealing(
                new SessionReasonForAppealing(
                    Collections.singletonList(
                        new SessionReasonForAppealingItem(
                            "Underpayment",
                            "I think I should get more")
                    )
                )
            )
            .otherReasonForAppealing(new SessionOtherReasonForAppealing("I can't think of anything else"))
            .evidenceProvide(new SessionEvidenceProvide("no"))
            .theHearing(new SessionTheHearing("yes"))
            .build();

        assertThatJson(SESSION_SAMPLE_ESA.getSerializedMessage())
            .when(Option.IGNORING_EXTRA_FIELDS)
            .isEqualTo(sessionDraft);
    }

    @Test
    public void shouldSerializeSessionDraftWithDatesCantAttendAsExpected() {
        SessionDraft sessionDraft = SessionDraft.builder()
            .benefitType(new SessionBenefitType("Personal Independence Payment (PIP)"))
            .postcode(new SessionPostcodeChecker("n29ed"))
            .createAccount(new SessionCreateAccount("yes"))
            .haveAMrn(new SessionHaveAMrn("yes"))
            .mrnDate(new SessionMrnDate(new SessionDate("10", "10", "1990")))
            .checkMrn(new SessionCheckMrn("yes"))
            .mrnOverThirteenMonthsLate(new SessionMrnOverThirteenMonthsLate("aassas dasdsa dasdasda das"))
            .dwpIssuingOffice(new SessionDwpIssuingOffice("1"))
            .appointee(new SessionAppointee("no"))
            .appellantName(new SessionName("Mrs.", "Ap", "Pellant"))
            .appellantDob(new SessionDob(new SessionDate("31", "12", "1998")))
            .appellantNino(new SessionAppellantNino("SC 94 27 06 A"))
            .appellantContactDetails(
                new SessionContactDetails(
                    "1 Appellant Close",
                    null,
                    "Appellant-town",
                    "Appellant-county",
                    null,
                    "TS1 1ST",
                    null,
                    "07911123456",
                    "appellant@gmail.com",
                    null,
                    null,
                    null
                )
            )
            .textReminders(new SessionTextReminders("yes"))
            .sendToNumber(new SessionSendToNumber("yes"))
            .representative(new SessionRepresentative("no"))
            .reasonForAppealing(
                new SessionReasonForAppealing(
                    Collections.singletonList(
                        new SessionReasonForAppealingItem(
                            "Underpayment",
                            "I think I should get more")
                    )
                )
            )
            .otherReasonForAppealing(new SessionOtherReasonForAppealing("I can't think of anything else"))
            .evidenceProvide(new SessionEvidenceProvide("no"))
            .theHearing(new SessionTheHearing("yes"))
            .hearingSupport(new SessionHearingSupport("yes"))
            .hearingAvailability(new SessionHearingAvailability("yes"))
            .hearingArrangements(
                new SessionHearingArrangements(
                    new SessionHearingArrangementsSelection(
                        new SessionHearingArrangement(true, "Spanish"),
                        new SessionHearingArrangement(true, "British Sign Language (BSL)"),
                        new SessionHearingArrangement(true),
                        new SessionHearingArrangement(true),
                        new SessionHearingArrangement(true, "Help with stairs"))
                )
            )
            .datesCantAttend(
                new SessionDatesCantAttend(
                    Arrays.asList(
                        new SessionDate("11","7","2099"),
                        new SessionDate("12","7","2099")
                    )
                )
            )
            .build();

        assertThatJson(SESSION_SAMPLE_WITH_DATES_CANT_ATTEND.getSerializedMessage())
            .isEqualTo(sessionDraft);
    }

    @Test
    public void shouldSerializeIbcSessionDraftAsExpected() {
        SessionDraft sessionDraft = SessionDraft.builder()
            .benefitType(new SessionBenefitType("Infected Blood Compensation"))
            .postcode(new SessionPostcodeChecker("n29ed"))
            .createAccount(new SessionCreateAccount("yes"))
            .haveAMrn(new SessionHaveAMrn("yes"))
            .mrnDate(new SessionMrnDate(new SessionDate("10", "10", "1990")))
            .mrnOverThirteenMonthsLate(new SessionMrnOverThirteenMonthsLate("aassas dasdsa dasdasda das"))
            .appellantName(new SessionName("Mrs.", "Ap", "Pellant"))
            .appellantDob(new SessionDob(new SessionDate("31", "12", "1998")))
            .appellantIbcaReference(new SessionAppellantIbcaReference("12341234"))
            .appellantInMainlandUk(new SessionInMainlandUk("yes"))
            .appellantIbcRole(new SessionAppellantIbcRole("myself"))
            .appellantContactDetails(
                new SessionContactDetails(
                    "1 Appellant Close",
                    null,
                    "Appellant-town",
                    "Appellant-county",
                    null,
                    "TS1 1ST",
                    null,
                    "07911123456",
                    "appellant@gmail.com",
                    null,
                    null,
                    null
                )
            )
            .representative(new SessionRepresentative("no"))
            .build();

        assertThatJson(SESSION_SAMPLE_IBC.getSerializedMessage())
            .isEqualTo(sessionDraft);
    }

    @Test
    public void shouldSerializeIbcInternationalSessionDraftAsExpected() {
        SessionDraft sessionDraft = SessionDraft.builder()
            .benefitType(new SessionBenefitType("Infected Blood Compensation"))
            .postcode(new SessionPostcodeChecker("n29ed"))
            .createAccount(new SessionCreateAccount("yes"))
            .haveAMrn(new SessionHaveAMrn("yes"))
            .mrnDate(new SessionMrnDate(new SessionDate("10", "10", "1990")))
            .mrnOverThirteenMonthsLate(new SessionMrnOverThirteenMonthsLate("aassas dasdsa dasdasda das"))
            .appellantName(new SessionName("Mrs.", "Ap", "Pellant"))
            .appellantDob(new SessionDob(new SessionDate("31", "12", "1998")))
            .appellantIbcaReference(new SessionAppellantIbcaReference("12341234"))
            .appellantInMainlandUk(new SessionInMainlandUk("no"))
            .appellantIbcRole(new SessionAppellantIbcRole("myself"))
            .appellantInternationalContactDetails(
                new SessionContactDetails(
                    "1 Appellant Close",
                    null,
                    "Appellant-town",
                    null,
                    "Iceland",
                    "TS1 1ST",
                    "some-port-code",
                    "07911123456",
                    "appellant@gmail.com",
                    null,
                    null,
                    null
                )
            )
            .representative(new SessionRepresentative("no"))
            .build();

        assertThatJson(SESSION_SAMPLE_IBC_INTERNATIONAL.getSerializedMessage())
            .isEqualTo(sessionDraft);
    }

    @Test
    public void shouldSerializeIbcRepSessionDraftAsExpected() {
        SessionDraft sessionDraft = SessionDraft.builder()
            .benefitType(new SessionBenefitType("Infected Blood Compensation"))
            .postcode(new SessionPostcodeChecker("n29ed"))
            .createAccount(new SessionCreateAccount("yes"))
            .haveAMrn(new SessionHaveAMrn("yes"))
            .mrnDate(new SessionMrnDate(new SessionDate("10", "10", "1990")))
            .mrnOverThirteenMonthsLate(new SessionMrnOverThirteenMonthsLate("aassas dasdsa dasdasda das"))
            .appellantName(new SessionName("Mrs.", "Ap", "Pellant"))
            .appellantDob(new SessionDob(new SessionDate("31", "12", "1998")))
            .appellantIbcaReference(new SessionAppellantIbcaReference("12341234"))
            .appellantInMainlandUk(new SessionInMainlandUk("yes"))
            .appellantIbcRole(new SessionAppellantIbcRole("myself"))
            .appellantContactDetails(
                new SessionContactDetails(
                    "1 Appellant Close",
                    null,
                    "Appellant-town",
                    "Appellant-county",
                    null,
                    "TS1 1ST",
                    null,
                    "07911123456",
                    "appellant@gmail.com",
                    null,
                    null,
                    null
                )
            )
            .representative(new SessionRepresentative("yes"))
            .representativeInMainlandUk(new SessionInMainlandUk("yes"))
            .representativeDetails(
                new SessionRepresentativeDetails(
                    new SessionRepName(
                        "Mr.",
                        "Re",
                        "Presentative"
                    ),
                    "rep-line1",
                    "rep-line2",
                    "rep-town-city",
                    "rep-county",
                    null,
                    "RE7 7ES",
                    "07222222222",
                    "representative@gmail.com",
                    null,
                    null,
                    null
                )
            )
            .build();

        assertThatJson(SESSION_SAMPLE_IBC_REP.getSerializedMessage())
            .isEqualTo(sessionDraft);
    }

    @Test
    public void shouldSerializeIbcInternationalRepSessionDraftAsExpected() {
        SessionDraft sessionDraft = SessionDraft.builder()
            .benefitType(new SessionBenefitType("Infected Blood Compensation"))
            .postcode(new SessionPostcodeChecker("n29ed"))
            .createAccount(new SessionCreateAccount("yes"))
            .haveAMrn(new SessionHaveAMrn("yes"))
            .mrnDate(new SessionMrnDate(new SessionDate("10", "10", "1990")))
            .mrnOverThirteenMonthsLate(new SessionMrnOverThirteenMonthsLate("aassas dasdsa dasdasda das"))
            .appellantName(new SessionName("Mrs.", "Ap", "Pellant"))
            .appellantDob(new SessionDob(new SessionDate("31", "12", "1998")))
            .appellantIbcaReference(new SessionAppellantIbcaReference("12341234"))
            .appellantInMainlandUk(new SessionInMainlandUk("yes"))
            .appellantIbcRole(new SessionAppellantIbcRole("myself"))
            .appellantContactDetails(
                new SessionContactDetails(
                    "1 Appellant Close",
                    null,
                    "Appellant-town",
                    "Appellant-county",
                    null,
                    "TS1 1ST",
                    null,
                    "07911123456",
                    "appellant@gmail.com",
                    null,
                    null,
                    null
                )
            )
            .representative(new SessionRepresentative("yes"))
            .representativeInMainlandUk(new SessionInMainlandUk("no"))
            .representativeInternationalDetails(
                new SessionRepresentativeDetails(
                    new SessionRepName(
                        "Mr.",
                        "Re",
                        "Presentative"
                    ),
                    "rep-line1",
                    "rep-line2",
                    "rep-town-city",
                    null,
                    "Iceland",
                    "RE7 7ES",
                    "07222222222",
                    "representative@gmail.com",
                    null,
                    null,
                    null
                )
            )
            .build();

        assertThatJson(SESSION_SAMPLE_IBC_REP_INTERNATIONAL.getSerializedMessage())
            .isEqualTo(sessionDraft);
    }
}