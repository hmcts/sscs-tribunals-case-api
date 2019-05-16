package uk.gov.hmcts.reform.sscs.model.draft;

import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static uk.gov.hmcts.reform.sscs.util.SerializeJsonMessageManager.*;

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
            .appellantName(new SessionAppellantName("Mrs.", "Ap", "Pellant"))
            .appellantDob(new SessionAppellantDob(new SessionDate("31", "12", "1998")))
            .appellantNino(new SessionAppellantNino("SC 94 27 06 A"))
            .appellantContactDetails(
                new SessionAppellantContactDetails(
                    "1 Appellant Close",
                    null,
                    "Appellant-town",
                    "Appellant-county",
                    "TS1 1ST",
                    "07911123456",
                    "appellant@gmail.com"
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

        assertThatJson(SESSION_SAMPLE.getSerializedMessage())
            .when(Option.IGNORING_EXTRA_FIELDS)
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
            .appellantName(new SessionAppellantName("Mrs.", "Ap", "Pellant"))
            .appellantDob(new SessionAppellantDob(new SessionDate("31", "12", "1998")))
            .appellantNino(new SessionAppellantNino("SC 94 27 06 A"))
            .appellantContactDetails(
                new SessionAppellantContactDetails(
                    "1 Appellant Close",
                    null,
                    "Appellant-town",
                    "Appellant-county",
                    "TS1 1ST",
                    "07911123456",
                    "appellant@gmail.com"
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

        assertThatJson(SESSION_SAMPLE_WITH_NO_MRN.getSerializedMessage())
            .when(Option.IGNORING_EXTRA_FIELDS)
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
            .appellantName(new SessionAppellantName("Mrs.", "Ap", "Pellant"))
            .appellantDob(new SessionAppellantDob(new SessionDate("31", "12", "1998")))
            .appellantNino(new SessionAppellantNino("SC 94 27 06 A"))
            .appellantContactDetails(
                new SessionAppellantContactDetails(
                    "1 Appellant Close",
                    null,
                    "Appellant-town",
                    "Appellant-county",
                    "TS1 1ST",
                    "07911123456",
                    "appellant@gmail.com"
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
                    "RE7 7ES",
                    "07222222222",
                    "representative@gmail.com"
                )
            )
            .build();

        assertThatJson(SESSION_SAMPLE_WITH_REP.getSerializedMessage())
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
            .appellantName(new SessionAppellantName("Mrs.", "Ap", "Pellant"))
            .appellantDob(new SessionAppellantDob(new SessionDate("31", "12", "1998")))
            .appellantNino(new SessionAppellantNino("SC 94 27 06 A"))
            .appellantContactDetails(
                new SessionAppellantContactDetails(
                    "1 Appellant Close",
                    null,
                    "Appellant-town",
                    "Appellant-county",
                    "TS1 1ST",
                    "07911123456",
                    "appellant@gmail.com"
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

}