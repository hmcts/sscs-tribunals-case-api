package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.gen.scenarios;

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.gen.GenTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class GenScenarioNonDescriptorRefusedTest {

    @Test
    public void testScenario() {

        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType("faceToFace")
                .attendedHearing(true)
                .presentingOfficerAttended(false)
                .dateOfDecision("2020-09-20")
                .isDescriptorFlow(false)
                .isAllowed(false)
                .isSetAside(false)
                .summaryOfOutcomeDecision("My summary")
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else").build();

        GenTemplateContent content = GenScenario.SCENARIO_NON_DESCRIPTOR.getContent(body);

        String expectedContent = """
                The appeal is refused.
                
                The decision made by the Secretary of State on 20/09/2020 is confirmed.
                
                My summary
                
                My first reasons
                
                My second reasons
                
                Something else
                
                This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative did not attend.
                
                """;

        Assertions.assertEquals(7, content.getComponents().size());

        Assertions.assertEquals(expectedContent, content.toString());

    }

}
