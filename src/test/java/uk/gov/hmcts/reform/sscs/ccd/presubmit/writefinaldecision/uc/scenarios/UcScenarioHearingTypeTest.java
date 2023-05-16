package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios;

import java.util.Arrays;
import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;


@RunWith(JUnitParamsRunner.class)
public class UcScenarioHearingTypeTest {

    @NamedParameters("hearingTypeParameters")
    @SuppressWarnings("unused")
    private Object[] allNextHearingTypeParameters() {
        return new Object[] {
            new Object[] {"triage", false, false, "The tribunal considered the appeal bundle to page A1.\n"},
            new Object[] {"faceToFace", true, true, "This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.\n"},
            new Object[] {"faceToFace", true, false, "This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle to page A1. First Tier Agency representative did not attend.\n"},
            new Object[] {"faceToFace", false, true, "Felix Sydney the appellant requested an oral hearing but did not attend today. First Tier Agency representative attended on behalf of the Respondent.\n"
                    + "\n"
                    + "Having considered the appeal bundle to page A1 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Felix Sydney of the hearing and that it is in the interests of justice to proceed today. \n"},
            new Object[] {"faceToFace", false, false, "Felix Sydney the appellant requested an oral hearing but did not attend today. First Tier Agency representative did not attend.\n"
                    + "\n"
                    + "Having considered the appeal bundle to page A1 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Felix Sydney of the hearing and that it is in the interests of justice to proceed today. \n"},
            new Object[] {"paper", false, false, "No party has objected to the matter being decided without a hearing.\n"
                    + "\n"
                    + "Having considered the appeal bundle to page A1 and the requirements of rules 2 and 27 of the Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that it is able to decide the case in this way.\n"},
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
    public void testScenario3(String hearingType, boolean appellantAttended, boolean presentingOfficerAttended, String expectedHearingType) {
        WriteFinalDecisionTemplateBody body =
                WriteFinalDecisionTemplateBody.builder()
                        .hearingType(hearingType)
                        .attendedHearing(appellantAttended)
                        .presentingOfficerAttended(presentingOfficerAttended)
                        .isAllowed(true)
                        .isSetAside(true)
                        .dateOfDecision("2020-09-20")
                        .ucNumberOfPoints(15)
                        .pageNumber("A1")
                        .appellantName("Felix Sydney")
                        .appointeeName("Lewis Carter")
                        .schedule9Paragraph4Applicable(true)
                        .supportGroupOnly(true)
                        .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                        .anythingElse("Something else").build();

        UcTemplateContent content = UcScenario.SCENARIO_3.getContent(body);

        String expectedContent = "The appeal is allowed.\n"
                + "\n"
                + "The decision made by the Secretary of State on 20/09/2020 is set aside.\n"
                + "\n"
                + "Felix Sydney is to be treated as having limited capability for work-related activity. The matter is now remitted to the Secretary of State to make a final decision upon entitlement to Universal Credit (UC).\n"
                + "\n"
                + "The Secretary of State has accepted that Felix Sydney has limited capability for work. This was not in issue.\n"
                + "\n"
                + "No activity or descriptor from Schedule 7 of the UC Regulations 2013 was satisfied but Schedule 9, paragraph 4 of the UC Regulations applied.\n"
                + "\n"
                + "The tribunal applied Schedule 9, paragraph 4 because there would be a substantial risk to the mental or physical health of any person if the appellant were found not to have limited capability for work-related activity.\n"
                + "\n"
                + "My first reasons\n"
                + "\n"
                + "My second reasons\n"
                + "\n"
                + "Something else\n"
                + "\n"
                + expectedHearingType
                + "\n";

        if (appellantAttended || "triage".equals(hearingType)) {
            Assert.assertEquals(10, content.getComponents().size());
        } else {
            Assert.assertEquals(11, content.getComponents().size());
        }

        Assert.assertEquals(expectedContent, content.toString());
    }
}
