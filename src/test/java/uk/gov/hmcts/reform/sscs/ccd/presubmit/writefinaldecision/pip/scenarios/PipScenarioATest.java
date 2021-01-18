package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class PipScenarioATest {

    @Test
    public void testScenario1() {

        List<Descriptor> mobilityDescriptors =
                Arrays.asList(Descriptor.builder()
                                .activityQuestionValue("12. Moving Around")
                                .activityAnswerValue("12")
                                .activityAnswerLetter("a").activityAnswerPoints(0).build());

        WriteFinalDecisionTemplateBody body =
                WriteFinalDecisionTemplateBody.builder()
                        .hearingType("faceToFace")
                        .attendedHearing(true)
                        .presentingOfficerAttended(false)
                        .dateOfDecision("2020-09-20")
                        .startDate("2020-12-17")
                        .mobilityNumberOfPoints(0)
                        .dailyLivingAwardRate("not considered")
                        .dailyLivingIsEntited(false)
                        .mobilityIsEntited(false)
                        .mobilityAwardRate("no award")
                    .pageNumber("A1")
                        .appellantName("Felix Sydney")
                        .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                        .anythingElse("Something else")
                        .mobilityDescriptors(mobilityDescriptors).build();

        PipTemplateContent content = PipScenario.SCENARIO_1.getContent(body);


        String expectedContent = "The appeal is refused.\n"
                + "\n"
                + "The decision made by the Secretary of State on 20/09/2020 in respect of Personal Independence Payment is confirmed.\n"
                + "\n"
                + "Only the mobility component was in issue on this appeal and the daily living component was not considered.\n"
                + "\n"
                + "Felix Sydney does not qualify for an award of the mobility component from 17/12/2020. They score 0 points. This is insufficient to meet the threshold for the test.\n"
                + "\n"
                + "12. Moving Around\ta.12\t0"
                + "\n"
                + "\n\n"
                + "My first reasons\n"
                + "\n"
                + "My second reasons\n"
                + "\n"
                + "Something else\n"
                + "\n"
                + "This has been an oral (face to face) hearing. Felix Sydney attended the hearing today and the Tribunal considered the appeal bundle to page A1. No Presenting Officer attended on behalf of the Respondent.\n"
                + "\n";

        Assert.assertEquals(9, content.getComponents().size());

        Assert.assertEquals(expectedContent, content.toString());

    }
    
}
