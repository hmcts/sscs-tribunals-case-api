package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

class EsaScenario5Test {

    @Test
    void testScenario5() {

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

        EsaTemplateContent content = EsaScenario.SCENARIO_5.getContent(body);

        String expectedContent = """
            The appeal is allowed.

            The decision made by the Secretary of State on 20/09/2020 is set aside.

            Felix Sydney has limited capability for work.

            In applying the Work Capability Assessment 9 points were scored from the activities and descriptors in Schedule 2 of the Employment and Support Allowance (ESA) Regulations 2008 made up as follows:

            Mobilising Unaided\tc.1\t9


            Felix Sydney does not have limited capability for work-related activity because no descriptor from Schedule 3 of the ESA Regulations applied. Regulation 35 did not apply.

            My first reasons

            My second reasons

            Something else

            This has been an oral (face to face) hearing. The following people attended: Felix Sydney the appellant and a representative from the First Tier Agency. The Tribunal considered the appeal bundle to page A1.

            """;

        assertEquals(10, content.getComponents().size());

        assertEquals(expectedContent, content.toString());

    }

    @Test
    void testScenario5_NoSchedule2Descriptors() {

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

        EsaTemplateContent content = EsaScenario.SCENARIO_5.getContent(body);

        String expectedContent = """
            The appeal is allowed.

            The decision made by the Secretary of State on 20/09/2020 is set aside.

            Felix Sydney has limited capability for work.

            In applying the Work Capability Assessment 0 points were scored from the activities and descriptors in Schedule 2 of the Employment and Support Allowance (ESA) Regulations 2008.

            Felix Sydney does not have limited capability for work-related activity because no descriptor from Schedule 3 of the ESA Regulations applied. Regulation 35 did not apply.

            My first reasons

            My second reasons

            Something else

            This has been an oral (face to face) hearing. The following people attended: Felix Sydney the appellant and a representative from the First Tier Agency. The Tribunal considered the appeal bundle to page A1.

            """;

        assertEquals(9, content.getComponents().size());

        assertEquals(expectedContent, content.toString());

    }
}
