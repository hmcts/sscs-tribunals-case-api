package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcTemplateContent;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios.UcScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class UcScenario4Test {

    @Test
    public void testScenario4() {
        List<Descriptor> schedule7Descriptors =
                Arrays.asList(Descriptor.builder()
                        .activityQuestionValue("12. Coping with change. Cannot cope with any change, due to cognitive impairment or mental disorder, to the extent that day to day life cannot be managed.").build());

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
                        .supportGroupOnly(true)
                        .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                        .anythingElse("Something else")
                        .ucSchedule7Descriptors(schedule7Descriptors).build();

        UcTemplateContent content = UcScenario.SCENARIO_4.getContent(body);

        String expectedContent = """
            The appeal is allowed.
            
            The decision made by the Secretary of State on 20/09/2020 is set aside.
            
            Felix Sydney has limited capability for work-related activity. The matter is now remitted to the \
            Secretary of State to make a final decision upon entitlement to Universal Credit (UC).
            
            The Secretary of State has accepted that Felix Sydney has limited capability for work. This was not in issue.
            
            The following activity and descriptor from Schedule 7 of the UC Regulations 2013 applied: 
            
            12. Coping with change. Cannot cope with any change, due to cognitive impairment or mental disorder, to the extent that day to day life cannot be managed.
            
            
            My first reasons
            
            My second reasons
            
            Something else
            
            This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.
            
            """;

        Assertions.assertEquals(10, content.getComponents().size());

        Assertions.assertEquals(expectedContent, content.toString());

    }
}
