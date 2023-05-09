package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios;

import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class PipScenarioNoAwardEnhancedRateTest {

    @Test
    public void testScenario() {

        List<Descriptor> dailyLivingDescriptors =
            Arrays.asList(Descriptor.builder()
                    .activityQuestionNumber("1")
                    .activityQuestionValue("1.Preparing Food")
                    .activityAnswerValue("Needs prompting to be able to either prepare or cook a simple meal.")
                    .activityAnswerLetter("d").activityAnswerPoints(2).build(),
                Descriptor.builder()
                    .activityQuestionNumber("2")
                    .activityQuestionValue("2.Taking Nutrition")
                    .activityAnswerValue("Needs prompting to be able to take nutrition.")
                    .activityAnswerLetter("d").activityAnswerPoints(4).build());

        List<Descriptor> mobilityDescriptors =
            Arrays.asList(Descriptor.builder()
                .activityQuestionNumber("12")
                .activityQuestionValue("12.Moving Around")
                .activityAnswerValue("Can stand and then move more than 1 metre but no more than 20 metres, either aided or unaided.")
                .activityAnswerLetter("e").activityAnswerPoints(8).build());

        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType("faceToFace")
                .attendedHearing(true)
                .presentingOfficerAttended(false)
                .dateOfDecision("2020-09-20")
                .startDate("2020-12-17")
                .dailyLivingIsEntited(false)
                .mobilityIsEntited(true)
                .mobilityIsSeverelyLimited(true)
                .isDescriptorFlow(true)
                .isAllowed(false)
                .isSetAside(false)
                .dailyLivingNumberOfPoints(6)
                .mobilityNumberOfPoints(12)
                .dailyLivingAwardRate("no award")
                .mobilityAwardRate("enhanced rate")
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .dailyLivingDescriptors(dailyLivingDescriptors)
                .mobilityDescriptors(mobilityDescriptors).build();

        PipTemplateContent content = PipScenario.SCENARIO_NO_AWARD_AWARD.getContent(body);

        String expectedContent = "The appeal is refused.\n"
            + "\n"
            + "The decision made by the Secretary of State on 20/09/2020 is confirmed.\n"
            + "\n"
            + "Felix Sydney is not entitled to the daily living component from 17/12/2020. They score 6 points. This is insufficient to meet the threshold for the test.\n"
            + "\n"
            + "1.Preparing Food\td.Needs prompting to be able to either prepare or cook a simple meal.\t2\n"
            + "2.Taking Nutrition\td.Needs prompting to be able to take nutrition.\t4\n"
            + "\n"
            + "\n"
            + "Felix Sydney is entitled to the mobility component at the enhanced rate from 17/12/2020 for an indefinite period.\n"
            + "\n"
            + "Felix Sydney is severely limited in their ability to mobilise. They score 12 points. They satisfy the following descriptors:\n"
            + "\n"
            + "12.Moving Around\te.Can stand and then move more than 1 metre but no more than 20 metres, either aided or unaided.\t8\n"
            + "\n\n"
            + "My first reasons\n"
            + "\n"
            + "My second reasons\n"
            + "\n"
            + "Something else\n"
            + "\n"
            + "This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative did not attend.\n"
            + "\n";

        Assert.assertEquals(11, content.getComponents().size());

        Assert.assertEquals(expectedContent, content.toString());

    }

}
