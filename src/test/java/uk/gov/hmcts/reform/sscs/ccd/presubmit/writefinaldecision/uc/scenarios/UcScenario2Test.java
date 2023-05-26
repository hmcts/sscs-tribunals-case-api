package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcTemplateContent;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios.UcScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class UcScenario2Test {

    @Test
    public void testScenario2() {
        WriteFinalDecisionTemplateBody body =
                WriteFinalDecisionTemplateBody.builder()
                        .hearingType("faceToFace")
                        .attendedHearing(true)
                        .presentingOfficerAttended(true)
                        .isAllowed(false)
                        .isSetAside(false)
                        .dateOfDecision("2020-09-20")
                        .ucNumberOfPoints(null)
                        .pageNumber("A1")
                        .appellantName("Felix Sydney")
                        .schedule9Paragraph4Applicable(false)
                        .supportGroupOnly(true)
                        .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                        .anythingElse("Something else").build();

        UcTemplateContent content = UcScenario.SCENARIO_2.getContent(body);

        String expectedContent = "The appeal is refused.\n"
                + "\n"
                + "The decision made by the Secretary of State on 20/09/2020 is confirmed.\n"
                + "\n"
                + "Felix Sydney continues to have limited capability for work but does not have limited capability for work-related activity and cannot be treated as having limited capability for work-related activity.\n"
                + "\n"
                + "This is because no descriptor from Schedule 7 of the Universal Credit (UC) Regulations 2013 applied. Schedule 9, paragraph 4 did not apply.\n"
                + "\n"
                + "The Secretary of State has accepted that Felix Sydney has limited capability for work. This was not in issue.\n"
                + "\n"
                + "My first reasons\n"
                + "\n"
                + "My second reasons\n"
                + "\n"
                + "Something else\n"
                + "\n"
                + "This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.\n"
                + "\n";

        Assert.assertEquals(9, content.getComponents().size());

        Assert.assertEquals(expectedContent, content.toString());

    }
}
