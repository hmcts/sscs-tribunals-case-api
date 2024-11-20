package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

@RunWith(JUnitParamsRunner.class)
public class EsaScenario10Test {

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"true, allowed, has limited capability for work", "false, refused, does not have limited capability for work and cannot be treated as having limited capability for work"})
    public void testScenario10(boolean isAllowed, String allowedText, String capabilityText) {

        List<Descriptor> schedule2Descriptors =
            Arrays.asList(Descriptor.builder()
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

        String expectedContent = ("""
            The appeal is %s.
            
            The decision made by the Secretary of State on 20/09/2020 is confirmed.
            
            This is the summary of outcome decision
            
            My first reasons
            
            My second reasons
            
            Something else
            
            This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.
            
            """).formatted(
            allowedText, capabilityText);

        assertEquals(7, content.getComponents().size());

        assertThat(content.toString(), is(expectedContent));

    }
}
