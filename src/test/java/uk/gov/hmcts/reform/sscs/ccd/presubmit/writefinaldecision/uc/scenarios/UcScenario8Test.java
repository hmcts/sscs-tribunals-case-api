package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios;

import static java.time.LocalDate.now;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.util.DateUtilities.today;

import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class UcScenario8Test {

    @Test
    public void testScenario8() {

        List<Descriptor> schedule6Descriptors =
            singletonList(Descriptor.builder()
                              .activityQuestionValue("Mobilising Unaided")
                              .activityAnswerValue("1")
                              .activityAnswerLetter("c").activityAnswerPoints(9).build());

        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType("faceToFace")
                .attendedHearing(true)
                .presentingOfficerAttended(true)
                .isAllowed(true)
                .isSetAside(true)
                .dateOfDecision("2020-09-20")
                .ucNumberOfPoints(9)
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .reasonsForDecision(asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .schedule8Paragraph4Applicable(true)
                .schedule9Paragraph4Applicable(true)
                .ucCapabilityAssessmentStartDate(now())
                .ucSchedule6Descriptors(schedule6Descriptors).build();

        UcTemplateContent content = UcScenario.SCENARIO_8.getContent(body);

        String expectedContent = """
            The appeal is allowed.
            
            The decision made by the Secretary of State on 20/09/2020 is set aside.
            
            Felix Sydney is to be treated as having limited capability for work and for work-related activity from %s.
            
            This is because insufficient points were scored under Schedule 6 of the Universal Credit (UC) Regulations 2013 to meet the threshold for the Work Capability Assessment and none of the Schedule 7 activities or descriptors were satisfied.
            
            Mobilising Unaided\tc.1\t9
            
            
            The tribunal applied Schedule 8, paragraph 4 and Schedule 9, paragraph 4 because there would be a substantial risk to the mental or physical health of any person if the appellant were found not to have limited capability for work and for work-related activity.
            
            My first reasons
            
            My second reasons
            
            Something else
            
            This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.
            
            """.formatted(today());

        assertThat(content.getComponents()).hasSize(10);
        assertThat(content.toString()).isEqualTo(expectedContent);
    }

    @Test
    public void testScenario8WhenNoSchedule6Descriptors() {

        List<Descriptor> schedule6Descriptors = emptyList();

        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType("faceToFace")
                .attendedHearing(true)
                .presentingOfficerAttended(true)
                .isAllowed(true)
                .isSetAside(true)
                .dateOfDecision("2020-09-20")
                .ucNumberOfPoints(0)
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .reasonsForDecision(asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .schedule8Paragraph4Applicable(true)
                .schedule9Paragraph4Applicable(true)
                .ucCapabilityAssessmentStartDate(now())
                .ucSchedule6Descriptors(schedule6Descriptors).build();

        UcTemplateContent content = UcScenario.SCENARIO_8.getContent(body);

        String expectedContent = """
            The appeal is allowed.
            
            The decision made by the Secretary of State on 20/09/2020 is set aside.
            
            Felix Sydney is to be treated as having limited capability for work and for work-related activity from %s.
            
            This is because insufficient points were scored under Schedule 6 of the Universal Credit (UC) Regulations 2013 to meet the threshold for the Work Capability Assessment and none of the Schedule 7 activities or descriptors were satisfied.
            
            The tribunal applied Schedule 8, paragraph 4 and Schedule 9, paragraph 4 because there would be a substantial risk to the mental or physical health of any person if the appellant were found not to have limited capability for work and for work-related activity.
            
            My first reasons
            
            My second reasons
            
            Something else
            
            This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.
            
            """.formatted(today());

        assertThat(content.getComponents()).hasSize(9);
        assertThat(content.toString()).isEqualTo(expectedContent);
    }
}