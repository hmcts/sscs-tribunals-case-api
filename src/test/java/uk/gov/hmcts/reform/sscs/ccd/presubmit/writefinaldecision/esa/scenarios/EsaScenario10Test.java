package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class EsaScenario10Test {

    @Test
    public void testScenario10() {

        List<Descriptor> schedule2Descriptors =
                Arrays.asList(Descriptor.builder()
                        .activityQuestionValue("Mobilising Unaided")
                        .activityAnswerValue("1")
                        .activityAnswerLetter("c").activityAnswerPoints(9).build());

        WriteFinalDecisionTemplateBody body =
                WriteFinalDecisionTemplateBody.builder()
                        .isAllowed(true)
                        .wcaAppeal(false)
                        .dateOfDecision("20/09/2020")
                        .esaNumberOfPoints(0)
                        .pageNumber("A1")
                        .appellantName("Felix Sydney")
                        .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                        .anythingElse("Something else")
                        .dwpReassessTheAward("noRecommendation")
                        .regulation29Applicable(true)
                        .esaSchedule2Descriptors(schedule2Descriptors).build();

        EsaTemplateContent content = EsaScenario.SCENARIO_10.getContent(body).get();

        String expectedContent = "The appeal is allowed.\n"
                + "\n"
                + "The decision made by the Secretary of State on 20/09/2020 is confirmed.\n"
                + "\n"
                + "My first reasons\n"
                + "\n"
                + "My second reasons\n"
                + "\n"
                + "Something else\n"
                + "\n"
                + "This has been an oral (face to face) hearing. Felix Sydney attended the hearing today and the tribunal considered the appeal bundle to page A1. A Presenting Officer attended on behalf of the Respondent.\n"
                + "\n"
                + "Any recommendation given below does not form part of the Tribunal's decision and is not binding on the Secretary of State. The Tribunal makes no recommendation as to when the Department should reassess Felix Sydney.\n\n";

        assertEquals(7, content.getComponents().size());

        assertThat(content.toString(), is(expectedContent));

    }
}
