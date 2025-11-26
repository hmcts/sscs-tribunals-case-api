package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios;

import static java.time.LocalDate.now;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.hmcts.reform.sscs.util.DateUtilities.today;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class UcScenario6Test {

    @Test
    public void testScenario6() {

        List<Descriptor> schedule6Descriptors =
            singletonList(Descriptor.builder()
                              .activityQuestionValue("Mobilising Unaided")
                              .activityAnswerValue("1")
                              .activityAnswerLetter("c").activityAnswerPoints(9).build());

        List<Descriptor> schedule7Descriptors =
            singletonList(Descriptor.builder()
                              .activityQuestionValue("My schedule 3 descriptor").build());

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
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .schedule8Paragraph4Applicable(true)
                .ucSchedule6Descriptors(schedule6Descriptors)
                .ucCapabilityAssessmentStartDate(now())
                .ucSchedule7Descriptors(schedule7Descriptors).build();

        UcTemplateContent content = UcScenario.SCENARIO_6.getContent(body);

        String expectedContent = """
            The appeal is allowed.
            
            The decision made by the Secretary of State on 20/09/2020 is set aside.
            
            Felix Sydney has limited capability for work and for work-related activity from %s.
            
            In applying the Work Capability Assessment 9 points were scored from the activities and descriptors in Schedule 6 of the Universal Credit (UC) Regulations 2013 made up as follows:
            
            Mobilising Unaided\tc.1\t9
            
            
            The following activity and descriptor from Schedule 7 of the UC Regulations applied: 
            
            My schedule 3 descriptor
            
            
            My first reasons
            
            My second reasons
            
            Something else
            
            This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.
            
            """.formatted(today());

        assertThat(content.getComponents()).hasSize(11);

        assertThat(content.toString()).isEqualTo(expectedContent);
    }

    @Test
    public void testScenario6NoSchedule6Descriptors() {

        List<Descriptor> schedule6Descriptors =
            List.of();

        List<Descriptor> schedule7Descriptors =
            singletonList(Descriptor.builder()
                              .activityQuestionValue("My schedule 3 descriptor").build());

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
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .schedule8Paragraph4Applicable(true)
                .ucSchedule6Descriptors(schedule6Descriptors)
                .ucCapabilityAssessmentStartDate(now())
                .ucSchedule7Descriptors(schedule7Descriptors).build();

        UcTemplateContent content = UcScenario.SCENARIO_6.getContent(body);

        String expectedContent = """
            The appeal is allowed.
            
            The decision made by the Secretary of State on 20/09/2020 is set aside.
            
            Felix Sydney has limited capability for work and for work-related activity from %s.
            
            In applying the Work Capability Assessment 0 points were scored from the activities and descriptors in Schedule 6 of the Universal Credit (UC) Regulations 2013.
            
            The following activity and descriptor from Schedule 7 of the UC Regulations applied: 
            
            My schedule 3 descriptor
            
            
            My first reasons
            
            My second reasons
            
            Something else
            
            This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.
            
            """.formatted(today());

        assertThat(content.getComponents()).hasSize(10);

        assertThat(content.toString()).isEqualTo(expectedContent);
    }
}