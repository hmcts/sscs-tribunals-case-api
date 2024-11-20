package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class PipScenarioStandardRateEnhancedRateTest {

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
                .activityAnswerValue("Can stand and then move more than 1 metre but no more than 20 metres, either aided or unaided.")
                .activityAnswerLetter("e").activityAnswerPoints(8).build());

        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType("faceToFace")
                .attendedHearing(true)
                .presentingOfficerAttended(false)
                .dateOfDecision("2020-09-20")
                .startDate("2020-12-17")
                .dailyLivingIsEntited(true)
                .mobilityIsEntited(true)
                .mobilityIsSeverelyLimited(true)
                .isDescriptorFlow(true)
                .isAllowed(false)
                .isSetAside(false)
                .dailyLivingNumberOfPoints(8)
                .mobilityNumberOfPoints(12)
                .dailyLivingAwardRate("standard rate")
                .mobilityAwardRate("enhanced rate")
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .dailyLivingDescriptors(dailyLivingDescriptors)
                .mobilityDescriptors(mobilityDescriptors).build();

        PipTemplateContent content = PipScenario.SCENARIO_AWARD_AWARD.getContent(body);

        String expectedContent = """
                The appeal is refused.
                
                The decision made by the Secretary of State on 20/09/2020 is confirmed.
                
                Felix Sydney is entitled to the daily living component at the standard rate from 17/12/2020 for an indefinite period.
                
                Felix Sydney has limited ability to carry out the activities of daily living set out below. They score 8 points. They satisfy the following descriptors:
                
                1.Preparing Food	f.Cannot prepare and cook food.	8
                2.Taking Nutrition	a.Can take nutrition unaided.	0
                
                
                Felix Sydney is entitled to the mobility component at the enhanced rate from 17/12/2020 for an indefinite period.
                
                Felix Sydney is severely limited in their ability to mobilise. They score 12 points. They satisfy the following descriptors:
                
                12.Moving Around	e.Can stand and then move more than 1 metre but no more than 20 metres, either aided or unaided.	8
                
                
                My first reasons
                
                My second reasons
                
                Something else
                
                This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative did not attend.
                
                """;

        Assertions.assertEquals(12, content.getComponents().size());

        Assertions.assertEquals(expectedContent, content.toString());

    }

}
