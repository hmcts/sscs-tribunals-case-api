package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class PipScenarioNonDescriptorRefusedTest {

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

        PipTemplateContent content = PipScenario.SCENARIO_NON_DESCRIPTOR.getContent(body);

        String expectedContent = "The appeal is refused.\n"
                + "\n"
                + "The decision made by the Secretary of State on 20/09/2020 is confirmed.\n"
                + "\n"
                + "My summary\n"
                + "\n"
                + "My first reasons\n"
                + "\n"
                + "My second reasons\n"
                + "\n"
                + "Something else\n"
                + "\n"
                + "This has been an oral (face to face) hearing. Felix Sydney attended the hearing today and the Tribunal considered the appeal bundle to page A1. No Presenting Officer attended on behalf of the Respondent.\n"
                + "\n";

        Assert.assertEquals(7, content.getComponents().size());

        Assert.assertEquals(expectedContent, content.toString());

    }

}
