package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class PipScenarioStandardRateStandardRateTest {

    @Test
    public void testScenario() {

        List<Descriptor> dailyLivingDescriptors =
            Arrays.asList(Descriptor.builder()
                    .activityQuestionNumber("1")
                    .activityQuestionValue("1.Preparing Food")
                    .activityAnswerValue("Cannot prepare and cook food.")
                    .activityAnswerLetter("f").activityAnswerPoints(8).build(),
                Descriptor.builder()
                    .activityQuestionNumber("2")
                    .activityQuestionValue("2.Taking Nutrition")
                    .activityAnswerValue("Can take nutrition unaided.")
                    .activityAnswerLetter("a").activityAnswerPoints(0).build());

        List<Descriptor> mobilityDescriptors =
            Arrays.asList(Descriptor.builder()
                .activityQuestionNumber("12")
                .activityQuestionValue("12.Moving Around")
                .activityAnswerValue("Can stand and then move unaided more than 20 metres but no more than 50 metres.")
                .activityAnswerLetter("c").activityAnswerPoints(8).build());

        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType("faceToFace")
                .attendedHearing(true)
                .presentingOfficerAttended(false)
                .dateOfDecision("2020-09-20")
                .startDate("2020-12-17")
                .dailyLivingIsEntited(true)
                .mobilityIsEntited(true)
                .isDescriptorFlow(true)
                .isAllowed(false)
                .isSetAside(false)
                .dailyLivingNumberOfPoints(8)
                .mobilityNumberOfPoints(8)
                .dailyLivingAwardRate("standard rate")
                .mobilityAwardRate("standard rate")
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .dailyLivingDescriptors(dailyLivingDescriptors)
                .mobilityDescriptors(mobilityDescriptors).build();

        PipTemplateContent content = PipScenario.SCENARIO_AWARD_AWARD.getContent(body);

        String expectedContent = "The appeal is refused.\n"
                + "\n"
                + "The decision made by the Secretary of State on 20/09/2020 is confirmed.\n"
                + "\n"
                + "Felix Sydney is entitled to the daily living component at the standard rate from 17/12/2020 for an indefinite period.\n"
                + "\n"
                + "Felix Sydney has limited ability to carry out the activities of daily living set out below. They score 8 points. They satisfy the following descriptors:\n"
                + "\n"
                + "1.Preparing Food\tf.Cannot prepare and cook food.\t8\n"
                + "2.Taking Nutrition\ta.Can take nutrition unaided.\t0\n"
                + "\n"
                + "\n"
                + "Felix Sydney is entitled to the mobility component at the standard rate from 17/12/2020 for an indefinite period.\n"
                + "\n"
                + "Felix Sydney is limited in their ability to mobilise. They score 8 points. They satisfy the following descriptors:\n"
                + "\n"
                + "12.Moving Around\tc.Can stand and then move unaided more than 20 metres but no more than 50 metres.\t8\n"
                + "\n\n"
                + "My first reasons\n"
                + "\n"
                + "My second reasons\n"
                + "\n"
                + "Something else\n"
                + "\n"
                + "This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative did not attend.\n"
                + "\n";

        Assert.assertEquals(12, content.getComponents().size());

        Assert.assertEquals(expectedContent, content.toString());

    }

}
