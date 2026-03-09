package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.gen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.gen.scenarios.GenScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

class GenTemplateContentTest {

    @Test
    void testAppellantAttended() {
        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType("faceToFace")
                .attendedHearing(true)
                .presentingOfficerAttended(false)
                .dateOfDecision("2020-09-20")
                .isDescriptorFlow(false)
                .isAllowed(true)
                .isSetAside(true)
                .summaryOfOutcomeDecision("My summary")
                .pageNumber("A1")
                .appellantName("Felix Sydney")
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

        assertEquals("This has been an oral (face to face) hearing. "
            + "The following people attended: Felix Sydney the appellant. "
            + "A representative from the First Tier Agency did not attend. "
            + "The Tribunal considered the appeal bundle to page A1.", content.getComponents().get(6).getContent());
    }

    @Test
    void testAppointeeOnCaseAndAttended() {
        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType("faceToFace")
                .attendedHearing(false)
                .presentingOfficerAttended(false)
                .dateOfDecision("2020-09-20")
                .isDescriptorFlow(false)
                .isAllowed(true)
                .isSetAside(true)
                .summaryOfOutcomeDecision("My summary")
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .appointeeOnCase(true)
                .appointeeAttended(true)
                .appointeeName("Sammy Brown")
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

        assertEquals("This has been an oral (face to face) hearing. "
            + "The following people attended: Sammy Brown the appointee. "
            + "A representative from the First Tier Agency did not attend. "
            + "The Tribunal considered the appeal bundle to page A1.", content.getComponents().get(6).getContent());
    }

    @Test
    void testAppointeeOnCaseButDidNotAttend() {
        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType("faceToFace")
                .attendedHearing(true)
                .presentingOfficerAttended(false)
                .dateOfDecision("2020-09-20")
                .isDescriptorFlow(false)
                .isAllowed(true)
                .isSetAside(true)
                .summaryOfOutcomeDecision("My summary")
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .appointeeOnCase(true)
                .appointeeAttended(false)
                .appointeeName("Sammy Brown")
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

        assertEquals("This has been an oral (face to face) hearing. "
            + "Sammy Brown the appointee and a representative from the First Tier Agency did not attend. "
            + "The Tribunal considered the appeal bundle to page A1.", content.getComponents().get(6).getContent());

        assertEquals("Having considered the appeal bundle to page A1"
            + " and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber)"
            + " Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Sammy Brown of the hearing"
            + " and that it is in the interests of justice to proceed today. ", content.getComponents().get(7).getContent()
        );
    }

    @Test
    void testAppointeeOnCaseAndAttendedWithSomeOtherPartiesAttendedSomeDidNot() {
        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType("faceToFace")
                .attendedHearing(true)
                .presentingOfficerAttended(false)
                .dateOfDecision("2020-09-20")
                .isDescriptorFlow(false)
                .isAllowed(true)
                .isSetAside(true)
                .summaryOfOutcomeDecision("My summary")
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .appointeeOnCase(true)
                .appointeeAttended(true)
                .appointeeName("Sammy Brown")
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

        assertEquals("This has been an oral (face to face) hearing. "
            + "The following people attended: Sammy Brown the appointee, "
            + "Georgy Jj the second respondent and Jany Gigi the third respondent. "
            + "Theo Jj the forth respondent, Reo Gigi the fifth respondent and "
            + "a representative from the First Tier Agency did not attend. "
            + "The Tribunal considered the appeal bundle to page A1.", content.getComponents().get(6).getContent());
    }

}
