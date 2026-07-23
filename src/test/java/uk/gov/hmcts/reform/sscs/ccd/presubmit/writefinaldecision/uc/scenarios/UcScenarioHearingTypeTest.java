package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class UcScenarioHearingTypeTest {

    private static Object[] allNextHearingTypeParameters() {
        return new Object[]{new Object[]{"triage", false, false, "The tribunal considered the appeal bundle to page A1.\n"}, new Object[]{"faceToFace", true, true, "This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal bundle" + " to page A1. First Tier Agency representative attended on behalf of the Respondent.\n"}, new Object[]{"faceToFace", true, false, "This has been an oral (face to face) hearing. Felix Sydney the appellant attended the hearing today and the Tribunal considered the appeal " + "bundle to page A1. First Tier Agency representative did not attend.\n"}, new Object[]{"faceToFace", false, true, "Felix Sydney the appellant requested an oral hearing but did not attend today. First Tier Agency representative attended on behalf of the " + "Respondent.\n" + "\n" + "Having considered the appeal bundle to page A1 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social " + "Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Felix Sydney of the hearing and that it " + "is in the interests of justice to proceed today. \n"}, new Object[]{"faceToFace", false, false, "Felix Sydney the appellant requested an oral hearing but did not attend today. First Tier Agency representative did not attend.\n" + "\n" + "Having considered the appeal bundle to page A1 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social " + "Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Felix Sydney of the hearing and that it " + "is in the interests of justice to proceed today. \n"}, new Object[]{"paper", false, false, "No party has objected to the matter being decided without a hearing.\n" + "\n" + "Having considered the appeal bundle to page A1 and the requirements of rules 2 and 27 of the Tribunal Procedure (First-tier Tribunal)(Social " + "Entitlement Chamber) Rules 2008 the Tribunal is satisfied that it is able to decide the case in this way.\n"}, new Object[]{"telephone", true, true, "This has been a remote hearing in the form of a telephone hearing. Felix Sydney the appellant attended and the Tribunal considered the appeal " + "bundle to page A1. First Tier Agency representative attended on behalf of the Respondent.\n"}, new Object[]{"telephone", true, false, "This has been a remote hearing in the form of a telephone hearing. Felix Sydney the appellant attended and the Tribunal considered the appeal " + "bundle to page A1. First Tier Agency representative did not attend.\n"}, new Object[]{"telephone", false, true, "This has been a remote hearing in the form of a telephone hearing. Felix Sydney the appellant did not attend the hearing today. First Tier Agency" + " representative attended on behalf of the Respondent.\n" + "\n" + "Having considered the appeal bundle to page A1 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social " + "Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Felix Sydney of the hearing and that it is" + " in the interests of justice to proceed today. \n"}, new Object[]{"telephone", false, false, "This has been a remote hearing in the form of a telephone hearing. Felix Sydney the appellant did not attend the hearing today. First Tier " + "Agency representative did not attend.\n" + "\n" + "Having considered the appeal bundle to page A1 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social " + "Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Felix Sydney of the hearing and that it " + "is in the interests of justice to proceed today. \n"}, new Object[]{"video", true, true, "This has been a remote hearing in the form of a video hearing. Felix Sydney the appellant attended and the Tribunal considered the appeal bundle to " + "page A1. First Tier Agency representative attended on behalf of the Respondent.\n"}, new Object[]{"video", true, false, "This has been a remote hearing in the form of a video hearing. Felix Sydney the appellant attended and the Tribunal considered the appeal bundle to " + "page A1. First Tier Agency representative did not attend.\n"}, new Object[]{"video", false, true, "This has been a remote hearing in the form of a video hearing. Felix Sydney the appellant did not attend the hearing today. First Tier Agency " + "representative attended on behalf of the Respondent.\n" + "\n" + "Having considered the appeal bundle to page A1 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social " + "Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Felix Sydney of the hearing and that it is in " + "the interests of justice to proceed today. \n"}, new Object[]{"video", false, false, "This has been a remote hearing in the form of a video hearing. Felix Sydney the appellant did not attend the hearing today. First Tier Agency " + "representative did not attend.\n" + "\n" + "Having considered the appeal bundle to page A1 and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social " + "Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify Felix Sydney of the hearing and that it is in" + " the interests of justice to proceed today. \n"},};
    }

    @ParameterizedTest
    @MethodSource("allNextHearingTypeParameters")
    public void testScenario3(String hearingType, boolean appellantAttended, boolean presentingOfficerAttended, String expectedHearingType) {
        WriteFinalDecisionTemplateBody body = WriteFinalDecisionTemplateBody.builder()
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
            .anythingElse("Something else")
            .build();

        UcTemplateContent content = UcScenario.SCENARIO_3.getContent(body);

        String expectedContent = """
            The appeal is allowed.
            
            The decision made by the Secretary of State on 20/09/2020 is set aside.
            
            Felix Sydney is to be treated as having limited capability for work-related activity.
            
            The Secretary of State has accepted that Felix Sydney has limited capability for work. This was not in issue.
            
            No activity or descriptor from Schedule 7 of the Universal Credit (UC) Regulations 2013 was satisfied but Schedule 9, paragraph 4 of the UC Regulations applied.
            
            The tribunal applied Schedule 9, paragraph 4 because there would be a substantial risk to the mental or physical health of any person if the appellant were found not to have limited capability for work-related activity.
            
            My first reasons
            
            My second reasons
            
            Something else
            
            %s
            """.formatted(expectedHearingType);

        if (appellantAttended || "triage".equals(hearingType)) {
            assertThat(content.getComponents()).hasSize(10);
        } else {
            assertThat(content.getComponents()).hasSize(11);
        }

        assertThat(content.toString()).isEqualTo(expectedContent);
    }
}