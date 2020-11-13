package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;


public class EsaScenario2Test {

    @Test
    public void testScenario2() {
        WriteFinalDecisionTemplateBody body =
                WriteFinalDecisionTemplateBody.builder()
                        .isAllowed(false)
                        .isSetAside(false)
                        .dateOfDecision("20/09/2020")
                        .esaNumberOfPoints(9)
                        .appellantName("Felix Sydney")
                        .regulation35Applicable(false)
                        .supportGroupOnly(true)
                        .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                        .anythingElse("Something else").build();

        EsaTemplateContent content = EsaScenario.SCENARIO_2.getContent(body).get();

        String expectedContent = "The appeal is refused.\n"
                + "\n"
                + "The decision made by the Secretary of State on 20/09/2020 is confirmed.\n"
                + "\n"
                + "Felix Sydney continues to have limited capability for work but does not have limited capability for work-related activity. This is because no descriptor from Schedule 3 of the Employment and Support Allowance (ESA) Regulations 2008 applied. Regulation 35 did not apply. The Secretary of State has accepted that Felix Sydney has limited capability for work. This was not in issue.\n"
                + "\n"
                + "My first reasons\n"
                + "\n"
                + "My second reasons\n"
                + "\n"
                + "Something else\n"
                + "\n";

        Assert.assertEquals(6, content.getComponents().size());

        Assert.assertEquals(expectedContent, content.toString());

    }
}
