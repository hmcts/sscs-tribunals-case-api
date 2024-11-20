package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class PipScenarioNotConsideredNoAwardTest {

    @Test
    public void testScenario() {

        List<Descriptor> mobilityDescriptors =
                Arrays.asList(Descriptor.builder()
                                .activityQuestionNumber("12")
                                .activityQuestionValue("12.Moving Around")
                                .activityAnswerValue("Can stand and then move more than 200 metres, either aided or unaided.")
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
                        .isDescriptorFlow(true)
                        .isAllowed(false)
                        .isSetAside(false)
                        .mobilityAwardRate("no award")
                    .pageNumber("A1")
                        .appellantName("Felix Sydney")
                        .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                        .anythingElse("Something else")
                        .mobilityDescriptors(mobilityDescriptors).build();

        PipTemplateContent content = PipScenario.SCENARIO_NOT_CONSIDERED_NO_AWARD.getContent(body);


        String expectedContent = """
            The appeal is refused.
            
            The decision made by the Secretary of State on 20/09/2020 is confirmed.
            
            Only the mobility component was in issue on this appeal and the daily living component was not considered.
            
            Felix Sydney does not qualify for an award of the mobility component from 17/12/2020. They score 0 points. This is insufficient to meet the threshold for the test.
            
            12.Moving Around	a.Can stand and then move more than 200 metres, either aided or unaided.	0
            
            
            My first reasons
            
            My second reasons
            
            Something else
            
            This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative did not attend.
            
            """;

        Assertions.assertEquals(9, content.getComponents().size());

        Assertions.assertEquals(expectedContent, content.toString());

    }

}
