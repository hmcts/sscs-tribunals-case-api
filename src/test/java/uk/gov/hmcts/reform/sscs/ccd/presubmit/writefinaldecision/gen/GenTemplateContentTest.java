package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.gen.scenarios.GenScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

class GenTemplateContentTest {

    public static final String APPELLANT_NAME = "Felix Sydney";
    public static final String APPOINTEE_NAME = "Sammy Brown";

    @ParameterizedTest
    @CsvSource(value = {"faceToFace:This has been an oral (face to face) hearing.",
        "telephone:This has been a remote hearing in the form of a telephone hearing.",
        "video:This has been a remote hearing in the form of a video hearing."}, delimiter = ':')
    void testAppointeeIsNotOnCaseAndAppellantAttended(String hearingType, String expectedHearingSentence) {
        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType(hearingType)
                .attendedHearing(true)
                .presentingOfficerAttended(false)
                .dateOfDecision("2020-09-20")
                .isDescriptorFlow(false)
                .isAllowed(true)
                .isSetAside(true)
                .summaryOfOutcomeDecision("My summary")
                .pageNumber("A1")
                .appellantName(APPELLANT_NAME)
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else").build();

        GenTemplateContent content = GenScenario.SCENARIO_NON_DESCRIPTOR.getContent(body);
        assertEquals("GEN", content.getBenefitTypeInitials());
        assertEquals("Generic Benefit Type", content.getBenefitTypeNameWithoutInitials());
        assertNull(content.getRegulationsYear());

        assertEquals(7, content.getComponents().size());
        assertEquals("The appeal is allowed.", content.getComponents().get(0).getContent());
        assertEquals("The decision made by the Secretary of State on 20/09/2020 is set aside.", content.getComponents().get(1).getContent());
        assertEquals("My summary", content.getComponents().get(2).getContent());
        assertEquals("My first reasons", content.getComponents().get(3).getContent());
        assertEquals("My second reasons", content.getComponents().get(4).getContent());
        assertEquals("Something else", content.getComponents().get(5).getContent());

        assertEquals(expectedHearingSentence
            + " The following people attended: Felix Sydney the appellant. "
            + "A representative from the First Tier Agency did not attend. "
            + "The Tribunal considered the appeal bundle to page A1.", content.getComponents().get(6).getContent());
    }

    @ParameterizedTest
    @CsvSource(value = {"faceToFace:This has been an oral (face to face) hearing.",
        "telephone:This has been a remote hearing in the form of a telephone hearing.",
        "video:This has been a remote hearing in the form of a video hearing."}, delimiter = ':')
    void testAppointeeIsNotOnCaseAndAppellantDidNotAttend(String hearingType, String expectedHearingSentence) {
        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType(hearingType)
                .attendedHearing(false)
                .presentingOfficerAttended(false)
                .dateOfDecision("2020-09-20")
                .isDescriptorFlow(false)
                .isAllowed(true)
                .isSetAside(true)
                .summaryOfOutcomeDecision("My summary")
                .pageNumber("A1")
                .appellantName(APPELLANT_NAME)
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else").build();

        GenTemplateContent content = GenScenario.SCENARIO_NON_DESCRIPTOR.getContent(body);
        assertEquals("GEN", content.getBenefitTypeInitials());
        assertEquals("Generic Benefit Type", content.getBenefitTypeNameWithoutInitials());
        assertNull(content.getRegulationsYear());

        assertEquals(8, content.getComponents().size());
        assertEquals("The appeal is allowed.", content.getComponents().get(0).getContent());
        assertEquals("The decision made by the Secretary of State on 20/09/2020 is set aside.", content.getComponents().get(1).getContent());
        assertEquals("My summary", content.getComponents().get(2).getContent());
        assertEquals("My first reasons", content.getComponents().get(3).getContent());
        assertEquals("My second reasons", content.getComponents().get(4).getContent());
        assertEquals("Something else", content.getComponents().get(5).getContent());

        assertEquals(expectedHearingSentence
            + " %s the appellant and a representative from the First Tier Agency did not attend.".formatted(APPELLANT_NAME),
            content.getComponents().get(6).getContent());

        assertEquals("Having considered the appeal bundle to page A1"
            + " and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber)"
            + " Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify %s of the hearing".formatted(APPELLANT_NAME)
            + " and that it is in the interests of justice to proceed today. ", content.getComponents().get(7).getContent());
    }

    @ParameterizedTest
    @CsvSource(value = {"faceToFace:This has been an oral (face to face) hearing.",
        "telephone:This has been a remote hearing in the form of a telephone hearing.",
        "video:This has been a remote hearing in the form of a video hearing."}, delimiter = ':')
    void testAppointeeOnCaseAndAttended(String hearingType, String expectedHearingSentence) {
        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType(hearingType)
                .attendedHearing(false)
                .presentingOfficerAttended(false)
                .dateOfDecision("2020-09-20")
                .isDescriptorFlow(false)
                .isAllowed(true)
                .isSetAside(true)
                .summaryOfOutcomeDecision("My summary")
                .pageNumber("A1")
                .appellantName(APPELLANT_NAME)
                .appointeeOnCase(true)
                .appointeeAttended(true)
                .appointeeName(APPOINTEE_NAME)
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else").build();

        GenTemplateContent content = GenScenario.SCENARIO_NON_DESCRIPTOR.getContent(body);
        assertEquals("GEN", content.getBenefitTypeInitials());
        assertEquals("Generic Benefit Type", content.getBenefitTypeNameWithoutInitials());
        assertNull(content.getRegulationsYear());

        assertEquals(7, content.getComponents().size());
        assertEquals("The appeal is allowed.", content.getComponents().get(0).getContent());
        assertEquals("The decision made by the Secretary of State on 20/09/2020 is set aside.", content.getComponents().get(1).getContent());
        assertEquals("My summary", content.getComponents().get(2).getContent());
        assertEquals("My first reasons", content.getComponents().get(3).getContent());
        assertEquals("My second reasons", content.getComponents().get(4).getContent());
        assertEquals("Something else", content.getComponents().get(5).getContent());

        assertEquals(expectedHearingSentence
            + " The following people attended: %s the appointee. ".formatted(APPOINTEE_NAME)
            + "A representative from the First Tier Agency did not attend. "
            + "The Tribunal considered the appeal bundle to page A1.", content.getComponents().get(6).getContent());
    }

    @ParameterizedTest
    @CsvSource(value = {"faceToFace:This has been an oral (face to face) hearing.",
        "telephone:This has been a remote hearing in the form of a telephone hearing.",
        "video:This has been a remote hearing in the form of a video hearing.",
        "triage:xxx"}, delimiter = ':')
    void testAppointeeOnCaseButDidNotAttend(String hearingType, String expectedHearingSentence) {
        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType(hearingType)
                .attendedHearing(true)
                .presentingOfficerAttended(false)
                .dateOfDecision("2020-09-20")
                .isDescriptorFlow(false)
                .isAllowed(true)
                .isSetAside(true)
                .summaryOfOutcomeDecision("My summary")
                .pageNumber("A1")
                .appellantName(APPELLANT_NAME)
                .appointeeOnCase(true)
                .appointeeAttended(false)
                .appointeeName(APPOINTEE_NAME)
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .build();

        GenTemplateContent content = GenScenario.SCENARIO_NON_DESCRIPTOR.getContent(body);
        assertEquals("GEN", content.getBenefitTypeInitials());
        assertEquals("Generic Benefit Type", content.getBenefitTypeNameWithoutInitials());
        assertNull(content.getRegulationsYear());

        assertEquals("The appeal is allowed.", content.getComponents().get(0).getContent());
        assertEquals("The decision made by the Secretary of State on 20/09/2020 is set aside.", content.getComponents().get(1).getContent());
        assertEquals("My summary", content.getComponents().get(2).getContent());
        assertEquals("My first reasons", content.getComponents().get(3).getContent());
        assertEquals("My second reasons", content.getComponents().get(4).getContent());
        assertEquals("Something else", content.getComponents().get(5).getContent());

        assertEquals(8, content.getComponents().size());

        assertEquals(expectedHearingSentence
            + " %s the appointee and a representative from the First Tier Agency did not attend.".formatted(APPOINTEE_NAME),
            content.getComponents().get(6).getContent());

        assertEquals("Having considered the appeal bundle to page A1"
            + " and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber)"
            + " Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify %s of the hearing".formatted(APPOINTEE_NAME)
            + " and that it is in the interests of justice to proceed today. ", content.getComponents().get(7).getContent()
        );
    }

    @Test
    void testTriageHearingTypeWhenAppointeeOnCaseButDidNotAttend() {
        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType("triage")
                .attendedHearing(true)
                .presentingOfficerAttended(false)
                .dateOfDecision("2020-09-20")
                .isDescriptorFlow(false)
                .isAllowed(true)
                .isSetAside(true)
                .summaryOfOutcomeDecision("My summary")
                .pageNumber("A1")
                .appellantName(APPELLANT_NAME)
                .appointeeOnCase(true)
                .appointeeAttended(false)
                .appointeeName(APPOINTEE_NAME)
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .otherPartyNamesAttendedHearing(List.of("Georgy Jj the second respondent", "Jany Gigi the third respondent"))
                .otherPartyNamesDidNotAttendHearing(List.of("Theo Jj the forth respondent", "Reo Gigi the fifth respondent"))
                .build();

        GenTemplateContent content = GenScenario.SCENARIO_NON_DESCRIPTOR.getContent(body);
        assertEquals("GEN", content.getBenefitTypeInitials());
        assertEquals("Generic Benefit Type", content.getBenefitTypeNameWithoutInitials());
        assertNull(content.getRegulationsYear());

        assertEquals("The appeal is allowed.", content.getComponents().get(0).getContent());
        assertEquals("The decision made by the Secretary of State on 20/09/2020 is set aside.", content.getComponents().get(1).getContent());
        assertEquals("My summary", content.getComponents().get(2).getContent());
        assertEquals("My first reasons", content.getComponents().get(3).getContent());
        assertEquals("My second reasons", content.getComponents().get(4).getContent());
        assertEquals("Something else", content.getComponents().get(5).getContent());
        assertEquals("The tribunal considered the appeal bundle to page A1.", content.getComponents().get(6).getContent());

        assertEquals(7, content.getComponents().size());

    }

    @ParameterizedTest
    @CsvSource(value = {"faceToFace:This has been an oral (face to face) hearing.",
        "telephone:This has been a remote hearing in the form of a telephone hearing.",
        "video:This has been a remote hearing in the form of a video hearing."}, delimiter = ':')
    void testAppointeeOnCaseButDidNotAttendAndTwoOtherPartiesAttendedButTwoOtherPartiesDidNotAttend(String hearingType, String expectedHearingSentence) {
        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType(hearingType)
                .attendedHearing(true)
                .presentingOfficerAttended(false)
                .dateOfDecision("2020-09-20")
                .isDescriptorFlow(false)
                .isAllowed(true)
                .isSetAside(true)
                .summaryOfOutcomeDecision("My summary")
                .pageNumber("A1")
                .appellantName(APPELLANT_NAME)
                .appointeeOnCase(true)
                .appointeeAttended(false)
                .appointeeName(APPOINTEE_NAME)
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .otherPartyNamesAttendedHearing(List.of("Georgy Jj the second respondent", "Jany Gigi the third respondent"))
                .otherPartyNamesDidNotAttendHearing(List.of("Theo Jj the forth respondent", "Reo Gigi the fifth respondent"))
                .build();

        GenTemplateContent content = GenScenario.SCENARIO_NON_DESCRIPTOR.getContent(body);
        assertEquals("GEN", content.getBenefitTypeInitials());
        assertEquals("Generic Benefit Type", content.getBenefitTypeNameWithoutInitials());
        assertNull(content.getRegulationsYear());

        assertEquals("The appeal is allowed.", content.getComponents().get(0).getContent());
        assertEquals("The decision made by the Secretary of State on 20/09/2020 is set aside.", content.getComponents().get(1).getContent());
        assertEquals("My summary", content.getComponents().get(2).getContent());
        assertEquals("My first reasons", content.getComponents().get(3).getContent());
        assertEquals("My second reasons", content.getComponents().get(4).getContent());
        assertEquals("Something else", content.getComponents().get(5).getContent());

        assertEquals(8, content.getComponents().size());

        assertEquals(expectedHearingSentence
            + " The following people attended: Georgy Jj the second respondent and Jany Gigi the third respondent. "
                + "%s the appointee, Theo Jj the forth respondent, Reo Gigi the fifth respondent ".formatted(APPOINTEE_NAME)
                + "and a representative from the First Tier Agency did not attend.",
            content.getComponents().get(6).getContent());

        assertEquals("Having considered the appeal bundle to page A1"
            + " and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber)"
            + " Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify %s of the hearing".formatted(APPOINTEE_NAME)
            + " and that it is in the interests of justice to proceed today. ", content.getComponents().get(7).getContent()
        );
    }

    @ParameterizedTest
    @CsvSource(value = {"faceToFace:This has been an oral (face to face) hearing.",
        "telephone:This has been a remote hearing in the form of a telephone hearing.",
        "video:This has been a remote hearing in the form of a video hearing."}, delimiter = ':')
    void testAppointeeOnCaseAndAttendedWithSomeOtherPartiesAttendedSomeDidNot(String hearingType, String expectedHearingSentence) {
        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType(hearingType)
                .attendedHearing(true)
                .presentingOfficerAttended(false)
                .dateOfDecision("2020-09-20")
                .isDescriptorFlow(false)
                .isAllowed(true)
                .isSetAside(true)
                .summaryOfOutcomeDecision("My summary")
                .pageNumber("A1")
                .appellantName(APPELLANT_NAME)
                .appointeeOnCase(true)
                .appointeeAttended(true)
                .appointeeName(APPOINTEE_NAME)
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .otherPartyNamesAttendedHearing(List.of("Georgy Jj the second respondent", "Jany Gigi the third respondent"))
                .otherPartyNamesDidNotAttendHearing(List.of("Theo Jj the forth respondent", "Reo Gigi the fifth respondent"))
                .build();

        GenTemplateContent content = GenScenario.SCENARIO_NON_DESCRIPTOR.getContent(body);
        assertEquals("GEN", content.getBenefitTypeInitials());
        assertEquals("Generic Benefit Type", content.getBenefitTypeNameWithoutInitials());
        assertNull(content.getRegulationsYear());

        assertEquals("The appeal is allowed.", content.getComponents().get(0).getContent());
        assertEquals("The decision made by the Secretary of State on 20/09/2020 is set aside.", content.getComponents().get(1).getContent());
        assertEquals("My summary", content.getComponents().get(2).getContent());
        assertEquals("My first reasons", content.getComponents().get(3).getContent());
        assertEquals("My second reasons", content.getComponents().get(4).getContent());
        assertEquals("Something else", content.getComponents().get(5).getContent());

        assertEquals(7, content.getComponents().size());

        assertEquals(expectedHearingSentence
            + " The following people attended: %s the appointee, ".formatted(APPOINTEE_NAME)
            + "Georgy Jj the second respondent and Jany Gigi the third respondent. "
            + "Theo Jj the forth respondent, Reo Gigi the fifth respondent and "
            + "a representative from the First Tier Agency did not attend. "
            + "The Tribunal considered the appeal bundle to page A1.", content.getComponents().get(6).getContent());
    }

}
