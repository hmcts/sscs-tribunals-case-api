package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

class EsaScenario7Test {

    @Test
    void testScenario7() {

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
                .isAllowed(true)
                .isSetAside(true)
                .dateOfDecision("2020-09-20")
                .esaNumberOfPoints(9)
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .regulation29Applicable(true)
                .esaSchedule2Descriptors(schedule2Descriptors).build();

        EsaTemplateContent content = EsaScenario.SCENARIO_7.getContent(body);

        String expectedContent = """
            The appeal is allowed.

            The decision made by the Secretary of State on 20/09/2020 is set aside.

            Felix Sydney is to be treated as having limited capability for work.

            This is because insufficient points were scored to meet the threshold for the Work Capability Assessment, but regulation 29 of the Employment and Support Allowance (ESA) Regulations 2008 applied.

            Mobilising Unaided\tc.1\t9


            The tribunal applied regulation 29 because there would be a substantial risk to the mental or physical health of any person if the appellant were found not to have limited capability for work.

            Felix Sydney does not have limited capability for work-related activity because no descriptor from Schedule 3 of the ESA Regulations applied. Regulation 35 did not apply.

            My first reasons

            My second reasons

            Something else

            This has been an oral (face to face) hearing. The following people attended: Felix Sydney the appellant and a representative from the First Tier Agency. The Tribunal considered the appeal bundle to page A1.

            """;

        assertEquals(11, content.getComponents().size());

        assertEquals(expectedContent, content.toString());

    }

    @Test
    void testScenario7WhenNoSchedule2() {

        List<Descriptor> schedule2Descriptors =
            List.of();

        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType("faceToFace")
                .attendedHearing(true)
                .presentingOfficerAttended(true)
                .isAllowed(true)
                .isSetAside(true)
                .dateOfDecision("2020-09-20")
                .esaNumberOfPoints(0)
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .regulation29Applicable(true)
                .esaSchedule2Descriptors(schedule2Descriptors).build();

        EsaTemplateContent content = EsaScenario.SCENARIO_7.getContent(body);

        String expectedContent = """
            The appeal is allowed.

            The decision made by the Secretary of State on 20/09/2020 is set aside.

            Felix Sydney is to be treated as having limited capability for work.

            This is because insufficient points were scored to meet the threshold for the Work Capability Assessment, but regulation 29 of the Employment and Support Allowance (ESA) Regulations 2008 applied.

            The tribunal applied regulation 29 because there would be a substantial risk to the mental or physical health of any person if the appellant were found not to have limited capability for work.

            Felix Sydney does not have limited capability for work-related activity because no descriptor from Schedule 3 of the ESA Regulations applied. Regulation 35 did not apply.

            My first reasons

            My second reasons

            Something else

            This has been an oral (face to face) hearing. The following people attended: Felix Sydney the appellant and a representative from the First Tier Agency. The Tribunal considered the appeal bundle to page A1.

            """;

        assertEquals(10, content.getComponents().size());

        assertEquals(expectedContent, content.toString());

    }
}
