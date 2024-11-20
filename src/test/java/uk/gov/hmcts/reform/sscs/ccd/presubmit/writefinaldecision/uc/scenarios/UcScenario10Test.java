package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcTemplateContent;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios.UcScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

@RunWith(JUnitParamsRunner.class)
public class UcScenario10Test {

    @Test
    // JunitParamsRunnerToParameterized conversion not supported
    @Parameters({"true, allowed", "false, refused"})
    public void testScenario10(boolean isAllowed, String allowedText) {

        List<Descriptor> schedule6Descriptors =
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
                .ucNumberOfPoints(0)
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .summaryOfOutcomeDecision("This is the summary of outcome decision")
                .schedule8Paragraph4Applicable(true)
                .ucSchedule6Descriptors(schedule6Descriptors).build();

        UcTemplateContent content = UcScenario.SCENARIO_10.getContent(body);

        String expectedContent = ("""
            The appeal is %s.
            
            The decision made by the Secretary of State on 20/09/2020 is confirmed.
            
            This is the summary of outcome decision
            
            My first reasons
            
            My second reasons
            
            Something else
            
            This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.
            
            """).formatted(
            allowedText);

        assertEquals(7, content.getComponents().size());

        assertThat(content.toString(), is(expectedContent));

    }
}
