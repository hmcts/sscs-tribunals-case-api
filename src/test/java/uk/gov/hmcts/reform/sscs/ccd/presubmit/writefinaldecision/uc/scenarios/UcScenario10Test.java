package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class UcScenario10Test {

    @ParameterizedTest
    @CsvSource({"true, allowed", "false, refused"})
    void testScenario10(boolean isAllowed, String allowedText) {

        List<Descriptor> schedule6Descriptors = singletonList(
            Descriptor.builder().activityQuestionValue("Mobilising Unaided").activityAnswerValue("1").activityAnswerLetter("c").activityAnswerPoints(9).build());

        WriteFinalDecisionTemplateBody body = WriteFinalDecisionTemplateBody.builder()
            .hearingType("faceToFace")
            .attendedHearing(true)
            .presentingOfficerAttended(true)
            .isAllowed(isAllowed)
            .wcaAppeal(false)
            .dateOfDecision("2020-09-20")
            .ucNumberOfPoints(0)
            .pageNumber("A1")
            .appellantName("Felix Sydney")
            .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
            .anythingElse("Something else")
            .summaryOfOutcomeDecision("This is the summary of outcome decision")
            .schedule8Paragraph4Applicable(true)
            .ucCapabilityAssessmentStartDate(LocalDate.of(2025, 11, 18))
            .ucSchedule6Descriptors(schedule6Descriptors)
            .build();

        UcTemplateContent content = UcScenario.SCENARIO_10.getContent(body);

        String expectedContent = """
            The appeal is %s.
            
            The decision made by the Secretary of State on 20/09/2020 is confirmed.
            
            This is the summary of outcome decision
            
            My first reasons
            
            My second reasons
            
            Something else
            
            This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.
            
            """.formatted(allowedText);

        assertThat(content.getComponents()).hasSize(7);
        assertThat(content.toString()).isEqualTo(expectedContent);
    }

    @ParameterizedTest
    @CsvSource({"true, allowed", "false, refused"})
    void testScenario10IsIbc(boolean isAllowed, String allowedText) {

        List<Descriptor> schedule6Descriptors = singletonList(
            Descriptor.builder().activityQuestionValue("Mobilising Unaided").activityAnswerValue("1").activityAnswerLetter("c").activityAnswerPoints(9).build());

        WriteFinalDecisionTemplateBody body = WriteFinalDecisionTemplateBody.builder()
            .hearingType("faceToFace")
            .attendedHearing(true)
            .presentingOfficerAttended(true)
            .isAllowed(isAllowed)
            .wcaAppeal(false)
            .dateOfDecision("2020-09-20")
            .ucNumberOfPoints(0)
            .pageNumber("A1")
            .appellantName("Felix Sydney")
            .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
            .anythingElse("Something else")
            .summaryOfOutcomeDecision("This is the summary of outcome decision")
            .schedule8Paragraph4Applicable(true)
            .isIbca(true)
            .ucSchedule6Descriptors(schedule6Descriptors)
            .build();

        UcTemplateContent content = UcScenario.SCENARIO_10.getContent(body);

        String expectedContent = """
            The appeal is %s.
            
            The decision made by the Infected Blood Compensation Authority on 20/09/2020 is confirmed.
            
            This is the summary of outcome decision
            
            My first reasons
            
            My second reasons
            
            Something else
            
            This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.
            
            """.formatted(allowedText);

        assertThat(content.getComponents()).hasSize(7);
        assertThat(content.toString()).isEqualTo(expectedContent);
    }
}