package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class PipScenarioNotConsideredEnhancedRateTest {

    @Test
    public void testScenario() {

        List<Descriptor> mobilityDescriptors =
                Arrays.asList(Descriptor.builder()
                                .activityQuestionNumber("12")
                                .activityQuestionValue("12.Moving Around")
                                .activityAnswerValue("Can stand and then move more than 1 metre but no more than 20 metres, either aided or unaided.")
                                .activityAnswerLetter("e").activityAnswerPoints(12).build());

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
                        .mobilityNumberOfPoints(12)
                        .isDescriptorFlow(true)
                        .isAllowed(false)
                        .isSetAside(false)
                        .dailyLivingAwardRate("not considered")
                        .mobilityAwardRate("enhanced rate")
                    .pageNumber("A1")
                        .appellantName("Felix Sydney")
                        .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                        .anythingElse("Something else")
                        .mobilityDescriptors(mobilityDescriptors).build();

        PipTemplateContent content = PipScenario.SCENARIO_NOT_CONSIDERED_AWARD.getContent(body);


        String expectedContent = """
            The appeal is refused.
            
            The decision made by the Secretary of State on 20/09/2020 is confirmed.
            
            Only the mobility component was in issue on this appeal and the daily living component was not considered.
            
            Felix Sydney is entitled to the mobility component at the enhanced rate from 17/12/2020 for an indefinite period.
            
            Felix Sydney is severely limited in their ability to mobilise. They score 12 points. They satisfy the following descriptors:
            
            12.Moving Around	e.Can stand and then move more than 1 metre but no more than 20 metres, either aided or unaided.	12
            
            
            My first reasons
            
            My second reasons
            
            Something else
            
            This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative did not attend.
            
            """;

        Assertions.assertEquals(10, content.getComponents().size());

        Assertions.assertEquals(expectedContent, content.toString());

    }

}
