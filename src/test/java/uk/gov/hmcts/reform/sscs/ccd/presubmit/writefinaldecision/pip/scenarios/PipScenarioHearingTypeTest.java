package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios;

import java.util.Arrays;
import java.util.List;
import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;


@RunWith(JUnitParamsRunner.class)
public class PipScenarioHearingTypeTest {

    @NamedParameters("hearingTypeParameters")
    @SuppressWarnings("unused")
    private Object[] allNextHearingTypeParameters() {
        // Lower case and the Tribunal
        return new Object[] {
            new Object[] {"faceToFace", true, true, "This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.\n"},
            new Object[] {"faceToFace", true, false, "This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative did not attend.\n"},
            new Object[] {"faceToFace", false, true, "Felix Sydney the appellant requested an oral hearing but did not attend today. First Tier Agency representative attended on behalf of the Respondent."
                    + "\n\n" + "Having considered the appeal bundle to page A1 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Felix Sydney of the hearing and that it is in the interests of justice to proceed today. \n"},
            new Object[] {"faceToFace", false, false, "Felix Sydney the appellant requested an oral hearing but did not attend today. First Tier Agency representative did not attend."
                    + "\n\n" + "Having considered the appeal bundle to page A1 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Felix Sydney of the hearing and that it is in the interests of justice to proceed today. \n"},
            new Object[] {"paper", false, false, "No party has objected to the matter being decided without a hearing.\n\nHaving considered the appeal bundle to page A1 and the requirements of rules 2 and 27 of the Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that it is able to decide the case in this way.\n"},
            new Object[] {"telephone", true, true, "This has been a remote hearing in the form of a telephone hearing. Felix Sydney the appellant attended and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.\n"},
            new Object[] {"telephone", true, false, "This has been a remote hearing in the form of a telephone hearing. Felix Sydney the appellant attended and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative did not attend.\n"},
            new Object[] {"telephone", false, true, "This has been a remote hearing in the form of a telephone hearing. Felix Sydney the appellant did not attend the hearing today. First Tier Agency representative attended on behalf of the Respondent.\n"
                    + "\n"
                    + "Having considered the appeal bundle to page A1 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Felix Sydney of the hearing and that it is in the interests of justice to proceed today. \n"},
            new Object[] {"telephone", false, false, "This has been a remote hearing in the form of a telephone hearing. Felix Sydney the appellant did not attend the hearing today. First Tier Agency representative did not attend.\n"
                    + "\n"
                    + "Having considered the appeal bundle to page A1 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Felix Sydney of the hearing and that it is in the interests of justice to proceed today. \n"},
            new Object[] {"video", true, true, "This has been a remote hearing in the form of a video hearing. Felix Sydney the appellant attended and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.\n"},
            new Object[] {"video", true, false, "This has been a remote hearing in the form of a video hearing. Felix Sydney the appellant attended and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative did not attend.\n"},
            new Object[] {"video", false, true, "This has been a remote hearing in the form of a video hearing. Felix Sydney the appellant did not attend the hearing today. First Tier Agency representative attended on behalf of the Respondent.\n"
                    + "\n"
                    + "Having considered the appeal bundle to page A1 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Felix Sydney of the hearing and that it is in the interests of justice to proceed today. \n"},
            new Object[] {"video", false, false, "This has been a remote hearing in the form of a video hearing. Felix Sydney the appellant did not attend the hearing today. First Tier Agency representative did not attend.\n"
                    + "\n"
                    + "Having considered the appeal bundle to page A1 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Felix Sydney of the hearing and that it is in the interests of justice to proceed today. \n"},
        };
    }


    @Test
    @Parameters(named = "hearingTypeParameters")
    public void testScenario(String hearingType, boolean appellantAttended, boolean presentingOfficerAttended, String expectedHearingType) {
        List<Descriptor> mobilityDescriptors =
            Arrays.asList(Descriptor.builder()
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

        String expectedContent = "The appeal is refused.\n"
            + "\n"
            + "The decision made by the Secretary of State on 20/09/2020 is confirmed.\n"
            + "\n"
            + "Only the mobility component was in issue on this appeal and the daily living component was not considered.\n"
            + "\n"
            + "Felix Sydney does not qualify for an award of the mobility component from 17/12/2020. They score 0 points. This is insufficient to meet the threshold for the test.\n"
            + "\n"
            + "12.Moving Around\ta.Can stand and then move more than 200 metres, either aided or unaided.\t0\n"
            + "\n"
            + "\n"
            + "My first reasons\n"
            + "\n"
            + "My second reasons\n"
            + "\n"
            + "Something else\n"
            + "\n"
            + expectedHearingType
            + "\n";

        Assert.assertEquals(appellantAttended ? 9 : 10, content.getComponents().size());

        Assert.assertEquals(expectedContent, content.toString());
    }
}
