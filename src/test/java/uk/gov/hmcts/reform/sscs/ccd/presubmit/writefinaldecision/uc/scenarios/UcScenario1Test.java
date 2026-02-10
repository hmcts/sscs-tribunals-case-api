package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.writefinaldecision.uc.UcTemplateContent;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.writefinaldecision.uc.scenarios.UcScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

class UcScenario1Test {

    @Test
    void testScenario1() {
        List<Descriptor> schedule6Descriptors = singletonList(Descriptor.builder()
            .activityQuestionValue("Mobilising Unaided")
            .activityAnswerValue("1")
            .activityAnswerLetter("c")
            .activityAnswerPoints(9)
            .build());

        WriteFinalDecisionTemplateBody body = WriteFinalDecisionTemplateBody.builder()
            .hearingType("faceToFace")
            .attendedHearing(true)
            .presentingOfficerAttended(true)
            .dateOfDecision("2020-09-20")
            .ucNumberOfPoints(9)
            .pageNumber("A1")
            .appellantName("Felix Sydney")
            .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
            .anythingElse("Something else")
            .ucSchedule6Descriptors(schedule6Descriptors)
            .build();

        UcTemplateContent content = UcScenario.SCENARIO_1.getContent(body);

        String expectedContent = """
            The appeal is refused.
            
            The decision made by the Secretary of State on 20/09/2020 is confirmed.
            
            Felix Sydney does not have limited capability for work and cannot be treated as having limited capability for work.
            
            In applying the Work Capability Assessment 9 points were scored from the activities and descriptors in Schedule 6 of the Universal Credit (UC) Regulations 2013. This is \
            insufficient to meet the threshold for the test. Schedule 8, paragraph 4 of the UC Regulations did not apply.
            
            Mobilising Unaided\tc.1\t9
            
            
            My first reasons
            
            My second reasons
            
            Something else
            
            This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. \
            First Tier Agency representative attended on behalf of the Respondent.
            
            """;

        assertThat(content.getComponents()).hasSize(9);
        assertThat(content.toString()).isEqualTo(expectedContent);
    }

    @Test
    void testScenario1WhenAnythingElseBlank() {
        List<Descriptor> schedule6Descriptors = singletonList(Descriptor.builder()
            .activityQuestionValue("Mobilising Unaided")
            .activityAnswerValue("1")
            .activityAnswerLetter("c")
            .activityAnswerPoints(9)
            .build());

        WriteFinalDecisionTemplateBody body = WriteFinalDecisionTemplateBody.builder()
            .hearingType("faceToFace")
            .attendedHearing(true)
            .presentingOfficerAttended(true)
            .dateOfDecision("2020-09-20")
            .ucNumberOfPoints(9)
            .pageNumber("A1")
            .appellantName("Felix Sydney")
            .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
            .anythingElse(" ")
            .ucSchedule6Descriptors(schedule6Descriptors)
            .build();

        UcTemplateContent content = UcScenario.SCENARIO_1.getContent(body);

        String expectedContent = """
            The appeal is refused.
            
            The decision made by the Secretary of State on 20/09/2020 is confirmed.
            
            Felix Sydney does not have limited capability for work and cannot be treated as having limited capability for work.
            
            In applying the Work Capability Assessment 9 points were scored from the activities and descriptors in Schedule 6 of the Universal Credit (UC) Regulations 2013. This is \
            insufficient to meet the threshold for the test. Schedule 8, paragraph 4 of the UC Regulations did not apply.
            
            Mobilising Unaided\tc.1\t9
            
            
            My first reasons
            
            My second reasons
            
            This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. \
            First Tier Agency representative attended on behalf of the Respondent.
            
            """;

        assertThat(content.getComponents()).hasSize(8);
        assertThat(content.toString()).isEqualTo(expectedContent);
    }

    @Test
    void testScenario1NoSchedule6() {
        List<Descriptor> schedule6Descriptors = emptyList();

        WriteFinalDecisionTemplateBody body = WriteFinalDecisionTemplateBody.builder()
            .hearingType("faceToFace")
            .attendedHearing(true)
            .presentingOfficerAttended(true)
            .dateOfDecision("2020-09-20")
            .ucNumberOfPoints(0)
            .pageNumber("A1")
            .appellantName("Felix Sydney")
            .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
            .anythingElse("Something else")
            .ucSchedule6Descriptors(schedule6Descriptors)
            .build();

        UcTemplateContent content = UcScenario.SCENARIO_1.getContent(body);

        String expectedContent = """
            The appeal is refused.
            
            The decision made by the Secretary of State on 20/09/2020 is confirmed.
            
            Felix Sydney does not have limited capability for work and cannot be treated as having limited capability for work.
            
            In applying the Work Capability Assessment 0 points were scored from the activities and descriptors in Schedule 6 of the Universal Credit (UC) Regulations 2013. This is \
            insufficient to meet the threshold for the test. Schedule 8, paragraph 4 of the UC Regulations did not apply.
            
            My first reasons
            
            My second reasons
            
            Something else
            
            This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. \
            First Tier Agency representative attended on behalf of the Respondent.
            
            """;

        assertThat(content.getComponents()).hasSize(8);
        assertThat(content.toString()).isEqualTo(expectedContent);
    }

    @Test
    void testScenario1IsIbc() {
        List<Descriptor> schedule6Descriptors = singletonList(Descriptor.builder()
            .activityQuestionValue("Mobilising Unaided")
            .activityAnswerValue("1")
            .activityAnswerLetter("c")
            .activityAnswerPoints(9)
            .build());

        WriteFinalDecisionTemplateBody body = WriteFinalDecisionTemplateBody.builder()
            .hearingType("faceToFace")
            .attendedHearing(true)
            .presentingOfficerAttended(true)
            .dateOfDecision("2020-09-20")
            .ucNumberOfPoints(9)
            .pageNumber("A1")
            .appellantName("Felix Sydney")
            .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
            .anythingElse("Something else")
            .isIbca(true)
            .ucSchedule6Descriptors(schedule6Descriptors)
            .build();

        UcTemplateContent content = UcScenario.SCENARIO_1.getContent(body);

        String expectedContent = """
            The appeal is refused.
            
            The decision made by the Infected Blood Compensation Authority on 20/09/2020 is confirmed.
            
            Felix Sydney does not have limited capability for work and cannot be treated as having limited capability for work.
            
            In applying the Work Capability Assessment 9 points were scored from the activities and descriptors in Schedule 6 of the Universal Credit (UC) Regulations 2013. This is \
            insufficient to meet the threshold for the test. Schedule 8, paragraph 4 of the UC Regulations did not apply.
            
            Mobilising Unaided\tc.1\t9
            
            
            My first reasons
            
            My second reasons
            
            Something else
            
            This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. \
            First Tier Agency representative attended on behalf of the Respondent.
            
            """;

        assertThat(content.getComponents()).hasSize(9);
        assertThat(content.toString()).isEqualTo(expectedContent);
    }
}
