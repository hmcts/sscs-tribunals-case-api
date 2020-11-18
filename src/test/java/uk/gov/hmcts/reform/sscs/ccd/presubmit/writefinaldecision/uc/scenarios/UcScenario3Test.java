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
                        .isAllowed(true)
                        .isSetAside(true)
                        .dateOfDecision("2020-09-20")
                        .ucNumberOfPoints(15)
                        .appellantName("Felix Sydney")
                        .regulation35Applicable(true)
                        .supportGroupOnly(true)
                        .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                        .anythingElse("Something else").build();

        UcTemplateContent content = UcScenario.SCENARIO_3.getContent(body);

        String expectedContent = "The appeal is allowed.\n"
                + "\n"
                + "The decision made by the Secretary of State on 20/09/2020 is set aside.\n"
                + "\n"
                + "Felix Sydney is to be treated as having limited capability for work-related activity.\n"
                + "\n"
                + "The Secretary of State has accepted that Felix Sydney has limited capability for work related activity. This was not an issue.\n"
                + "\n"
                + "No descriptor from Schedule 3 of the Employment and Support Allowance (ESA) Regulations 2008 was satisfied but regulation 35 applied.\n"
                + "\n"
                + "The tribunal applied that regulation because it found that Felix Sydney suffers from [insert disease or disablement] and, by reasons of such disease or disablement, there would be a substantial risk to the mental or physical health of any person if they were found not to have limited capability for work-related activity.\n"
                + "\n"
                + "My first reasons\n"
                + "\n"
                + "My second reasons\n"
                + "\n"
                + "Something else\n"
                + "\n";

        Assert.assertEquals(9, content.getComponents().size());

        Assert.assertEquals(expectedContent, content.toString());

    }
}
