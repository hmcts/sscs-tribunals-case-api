package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class EsaScenario7Test {

    @Test
    public void testScenario7() {

        List<Descriptor> schedule2Descriptors =
            Arrays.asList(Descriptor.builder()
                .activityQuestionValue("Mobilising Unaided")
                .activityAnswerValue("1")
                .activityAnswerLetter("c").activityAnswerPoints(9).build());

        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType("faceToFace")
                .attendedHearing(true)
                .presentingOfficerAttended(true)
                .isAllowed(true)
                .isSetAside(true)
                .dateOfDecision("2020-09-20")
                .esaNumberOfPoints(9)
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .regulation29Applicable(true)
                .esaSchedule2Descriptors(schedule2Descriptors).build();

        EsaTemplateContent content = EsaScenario.SCENARIO_7.getContent(body);

        String expectedContent = """
            The appeal is allowed.
            
            The decision made by the Secretary of State on 20/09/2020 is set aside.
            
            Felix Sydney is to be treated as having limited capability for work.
            
            This is because insufficient points were scored to meet the threshold for the Work Capability Assessment, but regulation 29 of the Employment and Support Allowance (ESA) Regulations 2008 applied.
            
            Mobilising Unaided	c.1	9
            
            
            The tribunal applied regulation 29 because there would be a substantial risk to the mental or physical health of any person if the appellant were found not to have limited capability for work.
            
            Felix Sydney does not have limited capability for work-related activity because no descriptor from Schedule 3 of the ESA Regulations applied. Regulation 35 did not apply.
            
            My first reasons
            
            My second reasons
            
            Something else
            
            This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.
            
            """;

        Assertions.assertEquals(11, content.getComponents().size());

        Assertions.assertEquals(expectedContent, content.toString());

    }

    @Test
    public void testScenario7WhenNoSchedule2() {

        List<Descriptor> schedule2Descriptors =
            Arrays.asList();

        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType("faceToFace")
                .attendedHearing(true)
                .presentingOfficerAttended(true)
                .isAllowed(true)
                .isSetAside(true)
                .dateOfDecision("2020-09-20")
                .esaNumberOfPoints(0)
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .regulation29Applicable(true)
                .esaSchedule2Descriptors(schedule2Descriptors).build();

        EsaTemplateContent content = EsaScenario.SCENARIO_7.getContent(body);

        String expectedContent = """
            The appeal is allowed.
            
            The decision made by the Secretary of State on 20/09/2020 is set aside.
            
            Felix Sydney is to be treated as having limited capability for work.
            
            This is because insufficient points were scored to meet the threshold for the Work Capability Assessment, but regulation 29 of the Employment and Support Allowance (ESA) Regulations 2008 applied.
            
            The tribunal applied regulation 29 because there would be a substantial risk to the mental or physical health of any person if the appellant were found not to have limited capability for work.
            
            Felix Sydney does not have limited capability for work-related activity because no descriptor from Schedule 3 of the ESA Regulations applied. Regulation 35 did not apply.
            
            My first reasons
            
            My second reasons
            
            Something else
            
            This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.
            
            """;

        Assertions.assertEquals(10, content.getComponents().size());

        Assertions.assertEquals(expectedContent, content.toString());

    }
}
