package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;


class PipScenarioHearingTypeTest {

    private static Stream<Arguments> allNextHearingTypeParameters() {
        return Stream.of(
            Arguments.of("faceToFace", true, true, "This has been an oral (face to face) hearing. The following people attended: Felix Sydney the appellant and a representative from the First Tier Agency. The Tribunal considered the appeal bundle to page A1.\n"),

            Arguments.of("faceToFace", true, false, "This has been an oral (face to face) hearing. The following people attended: Felix Sydney the appellant. A representative from the First Tier Agency did not attend. The Tribunal considered the appeal bundle to page A1.\n"),

            Arguments.of("faceToFace", false, true, """
                This has been an oral (face to face) hearing. The following people attended: A representative from the First Tier Agency. Felix Sydney the appellant did not attend.\


                Having considered the appeal bundle to page A1 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Felix Sydney of the hearing and that it is in the interests of justice to proceed today.\s
                """),

            Arguments.of("faceToFace", false, false, """
                This has been an oral (face to face) hearing. Felix Sydney the appellant and a representative from the First Tier Agency did not attend.\


                Having considered the appeal bundle to page A1 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Felix Sydney of the hearing and that it is in the interests of justice to proceed today.\s
                """),

            Arguments.of("paper", false, false, "No party has objected to the matter being decided without a hearing.\n\nHaving considered the appeal bundle to page A1 and the requirements of rules 2 and 27 of the Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that it is able to decide the case in this way.\n"),

            Arguments.of("telephone", true, true, "This has been a remote hearing in the form of a telephone hearing. The following people attended: Felix Sydney the appellant and a representative from the First Tier Agency. The Tribunal considered the appeal bundle to page A1.\n"),

            Arguments.of("telephone", true, false, "This has been a remote hearing in the form of a telephone hearing. The following people attended: Felix Sydney the appellant. A representative from the First Tier Agency did not attend. The Tribunal considered the appeal bundle to page A1.\n"),

            Arguments.of("telephone", false, true, """
                This has been a remote hearing in the form of a telephone hearing. The following people attended: A representative from the First Tier Agency. Felix Sydney the appellant did not attend.

                Having considered the appeal bundle to page A1 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Felix Sydney of the hearing and that it is in the interests of justice to proceed today.\s
                """),

            Arguments.of("telephone", false, false, """
                This has been a remote hearing in the form of a telephone hearing. Felix Sydney the appellant and a representative from the First Tier Agency did not attend.

                Having considered the appeal bundle to page A1 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Felix Sydney of the hearing and that it is in the interests of justice to proceed today.\s
                """),

            Arguments.of("video", true, true, "This has been a remote hearing in the form of a video hearing. The following people attended: Felix Sydney the appellant and a representative from the First Tier Agency. The Tribunal considered the appeal bundle to page A1.\n"),

            Arguments.of("video", true, false, "This has been a remote hearing in the form of a video hearing. The following people attended: Felix Sydney the appellant. A representative from the First Tier Agency did not attend. The Tribunal considered the appeal bundle to page A1.\n"),

            Arguments.of("video", false, true, """
                This has been a remote hearing in the form of a video hearing. The following people attended: A representative from the First Tier Agency. Felix Sydney the appellant did not attend.

                Having considered the appeal bundle to page A1 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Felix Sydney of the hearing and that it is in the interests of justice to proceed today.\s
                """),

            Arguments.of("video", false, false, """
                This has been a remote hearing in the form of a video hearing. Felix Sydney the appellant and a representative from the First Tier Agency did not attend.

                Having considered the appeal bundle to page A1 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Felix Sydney of the hearing and that it is in the interests of justice to proceed today.\s
                """)
            );
    }


    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    void testScenario(String hearingType, boolean appellantAttended, boolean presentingOfficerAttended, String expectedHearingType) {
        List<Descriptor> mobilityDescriptors =
            Collections.singletonList(Descriptor.builder()
                .activityQuestionNumber("12")
                .activityQuestionValue("12.Moving Around")
                .activityAnswerValue("Can stand and then move more than 200 metres, either aided or unaided.")
                .activityAnswerLetter("a").activityAnswerPoints(0).build());

        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType(hearingType)
                .attendedHearing(appellantAttended)
                .presentingOfficerAttended(presentingOfficerAttended)
                .dateOfDecision("2020-09-20")
                .startDate("2020-12-17")
                .mobilityNumberOfPoints(0)
                .dailyLivingAwardRate("not considered")
                .dailyLivingIsEntited(false)
                .mobilityIsEntited(false)
                .mobilityAwardRate("no award")
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .appointeeName("Lewis Carter")
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else")
                .mobilityDescriptors(mobilityDescriptors).build();

        PipTemplateContent content = PipScenario.SCENARIO_NOT_CONSIDERED_NO_AWARD.getContent(body);

        String expectedContent = """
            The appeal is refused.

            The decision made by the Secretary of State on 20/09/2020 is confirmed.

            Only the mobility component was in issue on this appeal and the daily living component was not considered.

            Felix Sydney does not qualify for an award of the mobility component from 17/12/2020. They score 0 points. This is insufficient to meet the threshold for the test.

            12.Moving Around\ta.Can stand and then move more than 200 metres, either aided or unaided.\t0


            My first reasons

            My second reasons

            Something else

            %s\

            """.formatted(expectedHearingType);

        assertEquals(appellantAttended ? 9 : 10, content.getComponents().size());

        assertEquals(expectedContent, content.toString());
    }
}
