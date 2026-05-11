package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

class EsaScenario10Test {

    @ParameterizedTest
    @CsvSource({"true, allowed", "false, refused"})
    void testScenario10(boolean isAllowed, String allowedText) {

        List<Descriptor> schedule2Descriptors =
            Collections.singletonList(Descriptor.builder()
                .activityQuestionValue("Mobilising Unaided")
                .activityAnswerValue("1")
                .activityAnswerLetter("c").activityAnswerPoints(9).build());

        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType("faceToFace")
                .attendedHearing(true)
                .presentingOfficerAttended(true)
                .isAllowed(isAllowed)
                .wcaAppeal(false)
                .dateOfDecision("2020-09-20")
                .esaNumberOfPoints(0)
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .summaryOfOutcomeDecision("This is the summary of outcome decision")
                .dwpReassessTheAward("noRecommendation")
                .regulation29Applicable(true)
                .esaSchedule2Descriptors(schedule2Descriptors).build();

        EsaTemplateContent content = EsaScenario.SCENARIO_10.getContent(body);

        String expectedContent = """
                The appeal is %s.
                
                The decision made by the Secretary of State on 20/09/2020 is confirmed.
                
                This is the summary of outcome decision
                
                My first reasons
                
                My second reasons
                
                Something else
                
                This has been an oral (face to face) hearing. The following people attended: Felix Sydney the appellant and a representative from the First Tier Agency. The Tribunal considered the appeal bundle to page A1.
                
                """.formatted(allowedText);

        Assertions.assertEquals(7, content.getComponents().size());

        assertThat(content.toString(), is(expectedContent));

    }
}
