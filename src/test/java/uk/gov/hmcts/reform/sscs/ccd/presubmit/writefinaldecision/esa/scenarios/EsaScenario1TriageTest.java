package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class EsaScenario1TriageTest {

    @Test
    public void testScenario1() {

        List<Descriptor> schedule2Descriptors =
            Arrays.asList(Descriptor.builder()
                .activityQuestionValue("Mobilising Unaided")
                .activityAnswerValue("1")
                .activityAnswerLetter("c").activityAnswerPoints(9).build());

        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType("triage")
                .attendedHearing(false)
                .presentingOfficerAttended(false)
                .dateOfDecision("2020-09-20")
                .esaNumberOfPoints(9)
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .esaSchedule2Descriptors(schedule2Descriptors).build();

        EsaTemplateContent content = EsaScenario.SCENARIO_1.getContent(body);

        String expectedContent = """
            The appeal is refused.
            
            The decision made by the Secretary of State on 20/09/2020 is confirmed.
            
            Felix Sydney does not have limited capability for work and cannot be treated as having limited capability for work.
            
            In applying the Work Capability Assessment 9 points were scored from the activities and descriptors in Schedule 2 of the Employment and Support Allowance (ESA) Regulations 2008. This is insufficient to meet the threshold for the test. Regulation 29 of the ESA Regulations did not apply.
            
            Mobilising Unaided	c.1	9
            
            
            My first reasons
            
            My second reasons
            
            Something else
            
            The tribunal considered the appeal bundle to page A1.
            
            """;

        Assertions.assertEquals(9, content.getComponents().size());

        Assertions.assertEquals(expectedContent, content.toString());

    }

    @Test
    public void testScenario1WhenAnythingElseBlank() {

        List<Descriptor> schedule2Descriptors =
            Arrays.asList(Descriptor.builder()
                .activityQuestionValue("Mobilising Unaided")
                .activityAnswerValue("1")
                .activityAnswerLetter("c").activityAnswerPoints(9).build());

        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType("triage")
                .attendedHearing(true)
                .presentingOfficerAttended(true)
                .dateOfDecision("2020-09-20")
                .esaNumberOfPoints(9)
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse(" ")
                .esaSchedule2Descriptors(schedule2Descriptors).build();

        EsaTemplateContent content = EsaScenario.SCENARIO_1.getContent(body);

        String expectedContent = """
            The appeal is refused.
            
            The decision made by the Secretary of State on 20/09/2020 is confirmed.
            
            Felix Sydney does not have limited capability for work and cannot be treated as having limited capability for work.
            
            In applying the Work Capability Assessment 9 points were scored from the activities and descriptors in Schedule 2 of the Employment and Support Allowance (ESA) Regulations 2008. This is insufficient to meet the threshold for the test. Regulation 29 of the ESA Regulations did not apply.
            
            Mobilising Unaided	c.1	9
            
            
            My first reasons
            
            My second reasons
            
            The tribunal considered the appeal bundle to page A1.
            
            """;

        Assertions.assertEquals(8, content.getComponents().size());

        Assertions.assertEquals(expectedContent, content.toString());

    }

    @Test
    public void testScenario1NoSchedule2() {

        List<Descriptor> schedule2Descriptors =
            Arrays.asList();

        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType("faceToFace")
                .attendedHearing(true)
                .presentingOfficerAttended(true)
                .dateOfDecision("2020-09-20")
                .esaNumberOfPoints(0)
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .esaSchedule2Descriptors(schedule2Descriptors).build();

        EsaTemplateContent content = EsaScenario.SCENARIO_1.getContent(body);

        String expectedContent = """
            The appeal is refused.
            
            The decision made by the Secretary of State on 20/09/2020 is confirmed.
            
            Felix Sydney does not have limited capability for work and cannot be treated as having limited capability for work.
            
            In applying the Work Capability Assessment 0 points were scored from the activities and descriptors in Schedule 2 of the Employment and Support Allowance (ESA) Regulations 2008. This is insufficient to meet the threshold for the test. Regulation 29 of the ESA Regulations did not apply.
            
            My first reasons
            
            My second reasons
            
            Something else
            
            This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.
            
            """;

        Assertions.assertEquals(8, content.getComponents().size());

        Assertions.assertEquals(expectedContent, content.toString());

    }
}
