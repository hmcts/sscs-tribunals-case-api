package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.writefinaldecision.uc.scenarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.writefinaldecision.uc.UcTemplateContent;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.writefinaldecision.uc.scenarios.UcScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class UcScenario3Test {

    @Test
    public void testScenario3() {
        WriteFinalDecisionTemplateBody body = WriteFinalDecisionTemplateBody.builder()
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
            .dwpReassessTheAward("doNotReassess12")
            .ucCapabilityAssessmentStartDate(LocalDate.of(2025, 11, 18))
            .build();

        UcTemplateContent content = UcScenario.SCENARIO_3.getContent(body);

        String expectedContent = """
            The appeal is allowed.
            
            The decision made by the Secretary of State on 20/09/2020 is set aside.
            
            Felix Sydney is to be treated as having limited capability for work-related activity from 18/11/2025.
            
            The Secretary of State has accepted that Felix Sydney has limited capability for work. This was not in issue.
            
            No activity or descriptor from Schedule 7 of the Universal Credit (UC) Regulations 2013 was satisfied but Schedule 9, paragraph 4 of the UC Regulations applied.
            
            The tribunal applied Schedule 9, paragraph 4 because there would be a substantial risk to the mental or physical health of any person if the appellant were found \
            not to have limited capability for work-related activity.
            
            My first reasons
            
            My second reasons
            
            Something else
            
            This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. \
            First Tier Agency representative attended on behalf of the Respondent.
            
            Any recommendation given below does not form part of the Tribunal's decision and is not binding on the Secretary of State. \
            The Tribunal recommends that the Department does not reassess Felix Sydney within 12 months from today's date.
            
            """;

        assertThat(content.getComponents()).hasSize(11);

        assertThat(content.toString()).isEqualTo(expectedContent);
    }
}
