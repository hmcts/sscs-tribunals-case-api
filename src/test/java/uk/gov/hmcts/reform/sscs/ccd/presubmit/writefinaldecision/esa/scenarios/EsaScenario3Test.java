package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios;

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
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
                        .dateOfDecision("2020-09-20")
                        .esaNumberOfPoints(null)
                        .pageNumber("A1")
                        .appellantName("Felix Sydney")
                        .regulation35Applicable(true)
                        .supportGroupOnly(true)
                        .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                        .anythingElse("Something else").build();

        EsaTemplateContent content = EsaScenario.SCENARIO_3.getContent(body);

        String expectedContent = """
                The appeal is allowed.
                
                The decision made by the Secretary of State on 20/09/2020 is set aside.
                
                Felix Sydney is to be treated as having limited capability for work-related activity.
                
                The Secretary of State has accepted that Felix Sydney has limited capability for work. This was not in issue.
                
                No activity or descriptor from Schedule 3 of the Employment and Support Allowance (ESA) Regulations 2008 was satisfied but regulation 35 of the ESA Regulations applied.
                
                The tribunal applied regulation 35 because there would be a substantial risk to the mental or physical health of any person if the appellant were found not to have limited capability for work-related activity.
                
                My first reasons
                
                My second reasons
                
                Something else
                
                This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.
                
                """;

        Assertions.assertEquals(10, content.getComponents().size());

        Assertions.assertEquals(expectedContent, content.toString());
    }
}
