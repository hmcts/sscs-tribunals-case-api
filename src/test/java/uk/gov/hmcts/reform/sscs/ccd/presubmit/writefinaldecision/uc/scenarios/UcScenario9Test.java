package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcTemplateContent;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios.UcScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class UcScenario9Test {

    @Test
    public void testScenario9() {

        List<Descriptor> schedule6Descriptors =
                singletonList(Descriptor.builder()
                        .activityQuestionValue("Mobilising Unaided")
                        .activityAnswerValue("1")
                        .activityAnswerLetter("c").activityAnswerPoints(9).build());

        List<Descriptor> schedule7Descriptors =
                singletonList(Descriptor.builder()
                        .activityQuestionValue("My schedule 7 descriptor").build());

        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType("faceToFace")
                .attendedHearing(true)
                .presentingOfficerAttended(true)
                .isAllowed(true)
                .isSetAside(true)
                .dateOfDecision("2020-09-20")
                .ucNumberOfPoints(9)
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .reasonsForDecision(asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .schedule8Paragraph4Applicable(true)
                .ucSchedule6Descriptors(schedule6Descriptors)
                .ucSchedule7Descriptors(schedule7Descriptors).build();

        UcTemplateContent content = UcScenario.SCENARIO_9.getContent(body);

        String expectedContent = "The appeal is allowed.\n"
            + "\n"
            + "The decision made by the Secretary of State on 20/09/2020 is set aside.\n"
            + "\n"
            + "Felix Sydney is to be treated as having limited capability for work and has limited capability for work-related activity. The matter is now remitted to the Secretary of State to make a final decision upon entitlement to Universal Credit (UC).\n"
            + "\n"
            + "This is because insufficient points were scored under Schedule 6 of the UC Regulations 2013 to meet the threshold for the Work Capability Assessment.\n"
            + "\n"
            + "Mobilising Unaided\tc.1\t9\n"
            + "\n"
            + "\n"
            + "The tribunal applied Schedule 8, paragraph 4 because there would be a substantial risk to the mental or physical health of any person if the appellant were found not to have limited capability for work.\n"
            + "\n"
            + "The following activity and descriptor from Schedule 7 of the UC Regulations applied: \n"
            + "\n"
            + "My schedule 7 descriptor\n"
            + "\n"
            + "\n"
            + "My first reasons\n"
            + "\n"
            + "My second reasons\n"
            + "\n"
            + "Something else\n"
            + "\n"
            + "This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.\n"
            + "\n";

        assertEquals(12, content.getComponents().size());

        assertEquals(expectedContent, content.toString());

    }
}
