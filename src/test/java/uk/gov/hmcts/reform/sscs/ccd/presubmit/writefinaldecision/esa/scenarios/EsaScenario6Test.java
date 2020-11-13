package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class EsaScenario6Test {

    @Test
    public void testScenario6() {

        List<Descriptor> schedule2Descriptors =
            Arrays.asList(Descriptor.builder()
                .activityQuestionValue("Mobilising Unaided")
                .activityAnswerValue("1")
                .activityAnswerLetter("c").activityAnswerPoints(9).build());

        List<Descriptor> schedule3Descriptors =
                Arrays.asList(Descriptor.builder()
                        .activityQuestionValue("My schedule 3 descriptor")
                        .activityAnswerValue("1")
                        .activityAnswerLetter("b").activityAnswerPoints(9).build());

        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .isAllowed(true)
                .isSetAside(true)
                .dateOfDecision("20/09/2020")
                .esaNumberOfPoints(9)
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .regulation29Applicable(true)
                .esaSchedule2Descriptors(schedule2Descriptors)
                .esaSchedule3Descriptors(schedule3Descriptors).build();

        EsaTemplateContent content = EsaScenario.SCENARIO_6.getContent(body).get();

        String expectedContent = "The appeal is allowed.\n"
            + "\n"
            + "The decision made by the Secretary of State on 20/09/2020 is set aside.\n"
            + "\n"
            + "Felix Sydney has limited capability for work and for work-related activity.\n"
            + "\n"
            + "In applying the work capability assessment 9 points were scored from the activities and descriptors in Schedule 2 of the ESA Regulations 2008 made up as follows:\n"
            + "\n"
            + "Mobilising Unaided\t1.c\t9\n"
            + "\n"
            + "\n"
            + "The following activity and descriptor from Schedule 3 applied:\n"
            + "\n"
            + "My schedule 3 descriptor\t1.b\n"
            + "\n"
            + "\n"
            + "My first reasons\n"
            + "\n"
            + "My second reasons\n"
            + "\n"
            + "Something else\n"
            + "\n"
            + "This has been an oral (face to face) hearing. Felix Sydney attended the hearing today and the tribunal considered the appeal bundle to page A1. A Presenting Officer attended on behalf of the Respondent.\n"
            + "\n";

        Assert.assertEquals(11, content.getComponents().size());

        Assert.assertEquals(expectedContent, content.toString());

    }
}
