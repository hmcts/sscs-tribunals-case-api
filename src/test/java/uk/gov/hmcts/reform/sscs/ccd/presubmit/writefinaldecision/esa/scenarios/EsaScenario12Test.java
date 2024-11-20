package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class EsaScenario12Test {

    @Test
    public void testScenario12() {

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
                .regulation35Applicable(true)
                .supportGroupOnly(false)
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .dwpReassessTheAward("noRecommendation")
                .esaSchedule2Descriptors(schedule2Descriptors).build();

        EsaTemplateContent content = EsaScenario.SCENARIO_12.getContent(body);

        String expectedContent = """
            The appeal is allowed.
            
            The decision made by the Secretary of State on 20/09/2020 is set aside.
            
            Felix Sydney has limited capability for work and is to be treated as having limited capability for work-related activity.
            
            In applying the Work Capability Assessment 9 points were scored from the activities and descriptors in Schedule 2 of the Employment and Support Allowance (ESA) Regulations 2008 made up as follows:
            
            Mobilising Unaided	c.1	9
            
            
            No activity or descriptor from Schedule 3 of the ESA Regulations was satisfied but regulation 35 of the ESA Regulations applied.
            
            The tribunal applied regulation 35 because there would be a substantial risk to the mental or physical health of any person if the appellant were found not to have limited capability for work-related activity.
            
            My first reasons
            
            My second reasons
            
            Something else
            
            This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.
            
            Any recommendation given below does not form part of the Tribunal's decision and is not binding on the Secretary of State. The Tribunal makes no recommendation as to when the Department should reassess Felix Sydney.
            
            """;

        Assertions.assertEquals(12, content.getComponents().size());

        Assertions.assertEquals(expectedContent, content.toString());

    }
}
