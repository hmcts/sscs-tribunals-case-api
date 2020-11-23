package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class UcScenario1Test {

    @Test
    public void testScenario1() {

        List<Descriptor> schedule6Descriptors =
            Arrays.asList(Descriptor.builder()
                .activityQuestionValue("Mobilising Unaided")
                .activityAnswerValue("1")
                .activityAnswerLetter("c").activityAnswerPoints(9).build());

        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType("faceToFace")
                .attendedHearing(true)
                .presentingOfficerAttended(true)
                .dateOfDecision("2020-09-20")
                .ucNumberOfPoints(9)
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .ucSchedule6Descriptors(schedule6Descriptors).build();

        UcTemplateContent content = UcScenario.SCENARIO_1.getContent(body);

        String expectedContent = "The appeal is refused.\n"
            + "\n"
            + "The decision made by the Secretary of State on 20/09/2020 is confirmed.\n"
            + "\n"
            + "Felix Sydney does not have limited capability for work and cannot be treated as having limited capability for work.\n"
            + "\n"
            + "In applying the work capability assessment 9 points were scored from the activities and descriptors in Schedule 2 of the ESA Regulations 2008. This is insufficient to meet the threshold for the test. Regulation 29 of the Employment and Support Allowance (ESA) Regulations 2008 did not apply.\n"
            + "\n"
            + "Mobilising Unaided\tc.1\t9\n"
            + "\n"
            + "\n"
            + "My first reasons\n"
            + "\n"
            + "My second reasons\n"
            + "\n"
            + "Something else\n"
            + "\n"
            + "This has been an oral (face to face) hearing. Felix Sydney attended the hearing today and the Tribunal considered the appeal bundle to page A1. A Presenting Officer attended on behalf of the Respondent.\n"
            + "\n";

        Assert.assertEquals(9, content.getComponents().size());

        Assert.assertEquals(expectedContent, content.toString());

    }

    @Test
    public void testScenario1NoSchedule6() {

        List<Descriptor> schedule6Descriptors =
            Arrays.asList();

        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType("faceToFace")
                .attendedHearing(true)
                .presentingOfficerAttended(true)
                .dateOfDecision("2020-09-20")
                .ucNumberOfPoints(0)
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .ucSchedule6Descriptors(schedule6Descriptors).build();

        UcTemplateContent content = UcScenario.SCENARIO_1.getContent(body);

        String expectedContent = "The appeal is refused.\n"
            + "\n"
            + "The decision made by the Secretary of State on 20/09/2020 is confirmed.\n"
            + "\n"
            + "Felix Sydney does not have limited capability for work and cannot be treated as having limited capability for work.\n"
            + "\n"
            + "In applying the work capability assessment 0 points were scored from the activities and descriptors in Schedule 2 of the ESA Regulations 2008. This is insufficient to meet the threshold for the test. Regulation 29 of the Employment and Support Allowance (ESA) Regulations 2008 did not apply.\n"
            + "\n"
            + "My first reasons\n"
            + "\n"
            + "My second reasons\n"
            + "\n"
            + "Something else\n"
            + "\n"
            + "This has been an oral (face to face) hearing. Felix Sydney attended the hearing today and the Tribunal considered the appeal bundle to page A1. A Presenting Officer attended on behalf of the Respondent.\n"
            + "\n";

        Assert.assertEquals(9, content.getComponents().size());

        Assert.assertEquals(expectedContent, content.toString());

    }
}
