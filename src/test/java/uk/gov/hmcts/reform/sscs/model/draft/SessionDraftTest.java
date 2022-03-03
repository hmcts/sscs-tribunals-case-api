package uk.gov.hmcts.reform.sscs.model.draft;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;
import static uk.gov.hmcts.reform.sscs.util.SerializeJsonMessageManager.SESSION_SAMPLE;
import static uk.gov.hmcts.reform.sscs.util.SerializeJsonMessageManager.SESSION_SAMPLE_ESA;
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
            .createAccount(new SessionCreateAccount(YES))
            .haveAMrn(new SessionHaveAMrn(YES))
            .mrnDate(new SessionMrnDate(new SessionDate("10", "10", "1990")))
            .checkMrn(new SessionCheckMrn(YES))
            .mrnOverThirteenMonthsLate(new SessionMrnOverThirteenMonthsLate("aassas dasdsa dasdasda das"))
            .dwpIssuingOffice(new SessionDwpIssuingOffice("1"))
            .appointee(new SessionAppointee(NO))
            .appellantName(new SessionName("Mrs.", "Ap", "Pellant"))
            .appellantDob(new SessionDob(new SessionDate("31", "12", "1998")))
            .appellantNino(new SessionAppellantNino("SC 94 27 06 A"))
            .appellantContactDetails(
                new SessionContactDetails(
                    "1 Appellant Close",
                    null,
                    "Appellant-town",
                    "Appellant-county",
                    "TS1 1ST",
                    "07911123456",
                    "appellant@gmail.com",
                    null,
                    null,
                    null
                )
            )
            .sameAddress(new SessionSameAddress(NO))
            .textReminders(new SessionTextReminders(YES))
            .sendToNumber(new SessionSendToNumber(YES))
            .representative(new SessionRepresentative(NO))
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
            .evidenceProvide(new SessionEvidenceProvide(NO))
            .theHearing(new SessionTheHearing(YES))
            .hearingSupport(new SessionHearingSupport(YES))
            .hearingAvailability(new SessionHearingAvailability(NO))
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
            .createAccount(new SessionCreateAccount(YES))
            .haveAMrn(new SessionHaveAMrn(NO))
            .haveContactedDwp(new SessionHaveContactedDwp(YES))
            .noMrn(new SessionNoMrn("I can't find my letter."))
            .dwpIssuingOffice(new SessionDwpIssuingOffice("1"))
            .appointee(new SessionAppointee(NO))
            .appellantName(new SessionName("Mrs.", "Ap", "Pellant"))
            .appellantDob(new SessionDob(new SessionDate("31", "12", "1998")))
            .appellantNino(new SessionAppellantNino("SC 94 27 06 A"))
            .appellantContactDetails(
                new SessionContactDetails(
                    "1 Appellant Close",
                    null,
                    "Appellant-town",
                    "Appellant-county",
                    "TS1 1ST",
                    "07911123456",
                    "appellant@gmail.com",
                    null,
                    null,
                    null
                )
            )
            .textReminders(new SessionTextReminders(YES))
            .sendToNumber(new SessionSendToNumber(YES))
            .representative(new SessionRepresentative(NO))
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
            .evidenceProvide(new SessionEvidenceProvide(NO))
            .theHearing(new SessionTheHearing(YES))
            .hearingSupport(new SessionHearingSupport(YES))
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
            .createAccount(new SessionCreateAccount(YES))
            .haveAMrn(new SessionHaveAMrn(YES))
            .mrnDate(new SessionMrnDate(new SessionDate("10", "10", "1990")))
            .checkMrn(new SessionCheckMrn(YES))
            .mrnOverThirteenMonthsLate(new SessionMrnOverThirteenMonthsLate("aassas dasdsa dasdasda das"))
            .dwpIssuingOffice(new SessionDwpIssuingOffice("1"))
            .appointee(new SessionAppointee(NO))
            .appellantName(new SessionName("Mrs.", "Ap", "Pellant"))
            .appellantDob(new SessionDob(new SessionDate("31", "12", "1998")))
            .appellantNino(new SessionAppellantNino("SC 94 27 06 A"))
            .appellantContactDetails(
                new SessionContactDetails(
                    "1 Appellant Close",
                    null,
                    "Appellant-town",
                    "Appellant-county",
                    "TS1 1ST",
                    "07911123456",
                    "appellant@gmail.com",
                    null,
                    null,
                    null
                )
            )
            .textReminders(new SessionTextReminders(YES))
            .sendToNumber(new SessionSendToNumber(YES))
            .representative(new SessionRepresentative(YES))
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
            .createAccount(new SessionCreateAccount(YES))
            .haveAMrn(new SessionHaveAMrn(YES))
            .mrnDate(new SessionMrnDate(new SessionDate("10", "10", "1990")))
            .checkMrn(new SessionCheckMrn(YES))
            .mrnOverThirteenMonthsLate(new SessionMrnOverThirteenMonthsLate("aassas dasdsa dasdasda das"))
            .dwpIssuingOffice(new SessionDwpIssuingOffice("1"))
            .appointee(new SessionAppointee(YES))
            .appointeeName(new SessionName("Mr.","Ap","Pointee"))
            .appointeeDob(new SessionDob(new SessionDate("1", "1", "1999")))
            .appointeeContactDetails(
                new SessionContactDetails(
                    "1 Appointee Street",
                    "",
                    "Appointee-town",
                    "Appointee-county",
                    "TS2 2ST",
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
                    "TS1 1ST",
                    "07911123456",
                    "appellant@gmail.com",
                    null,
                    null,
                    null
                )
            )
            .sameAddress(new SessionSameAddress(NO))
            .textReminders(new SessionTextReminders(YES))
            .sendToNumber(new SessionSendToNumber(YES))
            .representative(new SessionRepresentative(NO))
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
            .evidenceProvide(new SessionEvidenceProvide(NO))
            .theHearing(new SessionTheHearing(YES))
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
            .createAccount(new SessionCreateAccount(YES))
            .haveAMrn(new SessionHaveAMrn(YES))
            .mrnDate(new SessionMrnDate(new SessionDate("10", "10", "1990")))
            .checkMrn(new SessionCheckMrn(YES))
            .mrnOverThirteenMonthsLate(new SessionMrnOverThirteenMonthsLate("aassas dasdsa dasdasda das"))
            .dwpIssuingOffice(new SessionDwpIssuingOffice("1"))
            .appointee(new SessionAppointee(YES))
            .appointeeName(new SessionName("Mr.","Ap","Pointee"))
            .appointeeDob(new SessionDob(new SessionDate("1", "1", "1999")))
            .appointeeContactDetails(
                new SessionContactDetails(
                    "1 Appointee Street",
                    "",
                    "Appointee-town",
                    "Appointee-county",
                    "TS2 2ST",
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
            .sameAddress(new SessionSameAddress(YES))
            .textReminders(new SessionTextReminders(YES))
            .sendToNumber(new SessionSendToNumber(YES))
            .representative(new SessionRepresentative(NO))
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
            .evidenceProvide(new SessionEvidenceProvide(NO))
            .theHearing(new SessionTheHearing(YES))
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
            .createAccount(new SessionCreateAccount(YES))
            .haveAMrn(new SessionHaveAMrn(YES))
            .mrnDate(new SessionMrnDate(new SessionDate("10", "10", "1990")))
            .checkMrn(new SessionCheckMrn(YES))
            .mrnOverThirteenMonthsLate(new SessionMrnOverThirteenMonthsLate("aassas dasdsa dasdasda das"))
            .dwpIssuingOfficeEsa(new SessionDwpIssuingOfficeEsa("Chesterfield DRT"))
            .appointee(new SessionAppointee(NO))
            .appellantName(new SessionName("Mrs.", "Ap", "Pellant"))
            .appellantDob(new SessionDob(new SessionDate("31", "12", "1998")))
            .appellantNino(new SessionAppellantNino("SC 94 27 06 A"))
            .appellantContactDetails(
                new SessionContactDetails(
                    "1 Appellant Close",
                    null,
                    "Appellant-town",
                    "Appellant-county",
                    "TS1 1ST",
                    "07911123456",
                    "appellant@gmail.com",
                    null,
                    null,
                    null
                )
            )
            .textReminders(new SessionTextReminders(YES))
            .sendToNumber(new SessionSendToNumber(YES))
            .representative(new SessionRepresentative(NO))
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
            .evidenceProvide(new SessionEvidenceProvide(NO))
            .theHearing(new SessionTheHearing(YES))
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
            .createAccount(new SessionCreateAccount(YES))
            .haveAMrn(new SessionHaveAMrn(YES))
            .mrnDate(new SessionMrnDate(new SessionDate("10", "10", "1990")))
            .checkMrn(new SessionCheckMrn(YES))
            .mrnOverThirteenMonthsLate(new SessionMrnOverThirteenMonthsLate("aassas dasdsa dasdasda das"))
            .dwpIssuingOffice(new SessionDwpIssuingOffice("1"))
            .appointee(new SessionAppointee(NO))
            .appellantName(new SessionName("Mrs.", "Ap", "Pellant"))
            .appellantDob(new SessionDob(new SessionDate("31", "12", "1998")))
            .appellantNino(new SessionAppellantNino("SC 94 27 06 A"))
            .appellantContactDetails(
                new SessionContactDetails(
                    "1 Appellant Close",
                    null,
                    "Appellant-town",
                    "Appellant-county",
                    "TS1 1ST",
                    "07911123456",
                    "appellant@gmail.com",
                    null,
                    null,
                    null
                )
            )
            .textReminders(new SessionTextReminders(YES))
            .sendToNumber(new SessionSendToNumber(YES))
            .representative(new SessionRepresentative(NO))
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
            .evidenceProvide(new SessionEvidenceProvide(NO))
            .theHearing(new SessionTheHearing(YES))
            .hearingSupport(new SessionHearingSupport(YES))
            .hearingAvailability(new SessionHearingAvailability(YES))
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

}
