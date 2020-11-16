package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class EsaScenario3Test {

    @Test
    public void testScenario3() {
        WriteFinalDecisionTemplateBody body =
                WriteFinalDecisionTemplateBody.builder()
                        .hearingType("faceToFace")
                        .attendedHearing(true)
                        .presentingOfficerAttended(true)
                        .isAllowed(true)
                        .isSetAside(true)
                        .dateOfDecision("20/09/2020")
                        .esaNumberOfPoints(15)
                        .pageNumber("A1")
                        .appellantName("Felix Sydney")
                        .regulation35Applicable(true)
                        .supportGroupOnly(true)
                        .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                        .anythingElse("Something else").build();

        EsaTemplateContent content = EsaScenario.SCENARIO_3.getContent(body);

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
                + "\n"
                + "This has been an oral (face to face) hearing. Felix Sydney attended the hearing today and the tribunal considered the appeal bundle to page A1. A Presenting Officer attended on behalf of the Respondent.\n"
                + "\n";

        Assert.assertEquals(10, content.getComponents().size());

        Assert.assertEquals(expectedContent, content.toString());
    }
}
