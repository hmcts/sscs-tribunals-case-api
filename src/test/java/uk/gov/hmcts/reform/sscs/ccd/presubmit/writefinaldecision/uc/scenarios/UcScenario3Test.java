package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcTemplateContent;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios.UcScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class UcScenario3Test {

    @Test
    public void testScenario3() {
        WriteFinalDecisionTemplateBody body =
                WriteFinalDecisionTemplateBody.builder()
                        .hearingType("faceToFace")
                        .attendedHearing(true)
                        .presentingOfficerAttended(true)
                        .isAllowed(true)
                        .isSetAside(true)
                        .dateOfDecision("2020-09-20")
                        .ucNumberOfPoints(null)
                        .pageNumber("A1")
                        .appellantName("Felix Sydney")
                        .schedule9Paragraph4Applicable(true)
                        .supportGroupOnly(true)
                        .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                        .anythingElse("Something else")
                        .dwpReassessTheAward("doNotReassess12").build();

        UcTemplateContent content = UcScenario.SCENARIO_3.getContent(body);

        String expectedContent = "The appeal is allowed.\n"
                + "\n"
                + "The decision made by the Secretary of State on 20/09/2020 is set aside.\n"
                + "\n"
                + "Felix Sydney is to be treated as having limited capability for work-related activity. The matter is now remitted to the Secretary of State to make a final decision upon entitlement to Universal Credit (UC).\n"
                + "\n"
                + "The Secretary of State has accepted that Felix Sydney has limited capability for work. This was not in issue.\n"
                + "\n"
                + "No activity or descriptor from Schedule 7 of the UC Regulations 2013 was satisfied but Schedule 9, paragraph 4 of the UC Regulations applied.\n"
                + "\n"
                + "The tribunal applied Schedule 9, paragraph 4 because there would be a substantial risk to the mental or physical health of any person if the appellant were found not to have limited capability for work-related activity.\n"
                + "\n"
                + "My first reasons\n"
                + "\n"
                + "My second reasons\n"
                + "\n"
                + "Something else\n"
                + "\n"
                + "This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.\n"
                + "\n"
                + "Any recommendation given below does not form part of the Tribunal's decision and is not binding on the Secretary of State. "
                + "The Tribunal recommends that the Department does not reassess Felix Sydney within 12 months from today's date.\n"
                + "\n";

        Assert.assertEquals(11, content.getComponents().size());

        Assert.assertEquals(expectedContent, content.toString());
    }
}
