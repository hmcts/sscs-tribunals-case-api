package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class PipScenario1Test {

    @Test
    public void testScenario1() {

        List<Descriptor> dailyLivingDescriptors =
                Arrays.asList(Descriptor.builder()
                                .activityQuestionNumber("1")
                                .activityQuestionValue("1.Preparing food")
                                .activityAnswerValue("Needs supervision or assistance to either prepare or cook a simple meal.")
                                .activityAnswerLetter("e").activityAnswerPoints(4).build(),
                        Descriptor.builder()
                                .activityQuestionNumber("2")
                                .activityQuestionValue("2.Taking nutrition")
                                .activityAnswerValue("Needs prompting to be able to take nutrition.")
                                .activityAnswerLetter("d").activityAnswerPoints(4).build());


        WriteFinalDecisionTemplateBody body =
                WriteFinalDecisionTemplateBody.builder()
                        .hearingType("faceToFace")
                        .attendedHearing(true)
                        .presentingOfficerAttended(false)
                        .dateOfDecision("2020-09-20")
                        .dailyLivingNumberOfPoints(8)
                        .pageNumber("A1")
                        .startDate("2020-10-10")
                        .dailyLivingNumberOfPoints(8)
                        .mobilityNumberOfPoints(0)
                        .dailyLivingIsEntited(true)
                        .mobilityIsEntited(true)
                        .dailyLivingAwardRate("standard rate")
                        .mobilityAwardRate("standard rate")
                        .appellantName("Felix Sydney")
                        .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                        .anythingElse("Something else")
                        .dailyLivingDescriptors(dailyLivingDescriptors).build();

        PipTemplateContent content = PipScenario.SCENARIO_1.getContent(body);

        String expectedContent = "The appeal is refused.\n"
                + "\n"
                + "The decision made by the Secretary of State on 20/09/2020 in respect of Personal Independence Payment is confirmed.\n"
                + "\n"
                + "Felix Sydney is entitled to the daily living component at the standard rate from 10/10/2020 for an indefinite period.\n"
                + "\n"
                + "Felix Sydney has limited ability to carry out the activities of daily living set out below. They score 8 points. They satisfy the following descriptors:\n"
                + "\n"
                + "1.Preparing food\te.Needs supervision or assistance to either prepare or cook a simple meal.\t4\n"
                + "2.Taking nutrition\td.Needs prompting to be able to take nutrition.\t4\n"
                + "\n"
                + "\n" + "Felix Sydney is entitled to the mobility component at the standard rate from 10/10/2020 for an indefinite period.\n"
                + "\n"
                + "Felix Sydney is limited in their ability to mobilise. They score 0 points.\n"
                + "\n"
                + "My first reasons\n"
                + "\n"
                + "My second reasons\n"
                + "\n"
                + "Something else\n"
                + "\n"
                + "This has been an oral (face to face) hearing. Felix Sydney attended the hearing today and the tribunal considered the appeal bundle to page A1. No Presenting Officer attended on behalf of the Respondent.\n"
                + "\n";

        Assert.assertEquals(11, content.getComponents().size());

        Assert.assertEquals(expectedContent, content.toString());

    }
    
}
