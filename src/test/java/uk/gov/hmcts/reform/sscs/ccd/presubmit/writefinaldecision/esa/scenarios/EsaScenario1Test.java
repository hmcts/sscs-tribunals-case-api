package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class EsaScenario1Test {

    @Test
    public void testScenario1() {

        List<Descriptor> schedule2Descriptors =
            Arrays.asList(Descriptor.builder()
                .activityQuestionValue("Mobilising Unaided")
                .activityAnswerValue("1")
                .activityAnswerLetter("c").activityAnswerPoints(9).build());

        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .dateOfDecision("20/09/2020")
                .esaNumberOfPoints(9)
                .appellantName("Felix Sydney")
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .esaSchedule2Descriptors(schedule2Descriptors).build();

        EsaTemplateContent content = EsaScenario.SCENARIO_1.getContent(body).get();

        String expectedContent = "The appeal is refused.\n"
            + "\n"
            + "The decision made by the Secretary of State on 20/09/2020 is confirmed.\n"
            + "\n"
            + "Felix Sydney does not have limited capability for work and cannot be treated as having limited capability for work.\n"
            + "\n"
            + "In applying the work capability assessment 9 points were scored from the activities and descriptors in Schedule 2 of the ESA Regulations 2008. This is insufficient to meet the threshold for the test.Regulation 29 of the Employment and Support Allowance (ESA) Regulations 2008 did not apply.\n"
            + "\n"
            + "Mobilising Unaided\t1.c\t9\n"
            + "\n"
            + "\n"
            + "My first reasons\n"
            + "\n"
            + "My second reasons\n"
            + "\n"
            + "Something else\n"
            + "\n";

        Assert.assertEquals(8, content.getComponents().size());

        Assert.assertEquals(expectedContent, content.toString());

    }
}
