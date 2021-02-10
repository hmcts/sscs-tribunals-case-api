package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.dla;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.dla.scenarios.DlaScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class DlaTemplateContentTest {

    @Test
    public void testConstructor() {
        WriteFinalDecisionTemplateBody body =
            WriteFinalDecisionTemplateBody.builder()
                .hearingType("faceToFace")
                .attendedHearing(true)
                .presentingOfficerAttended(false)
                .dateOfDecision("2020-09-20")
                .isDescriptorFlow(false)
                .isAllowed(true)
                .isSetAside(true)
                .summaryOfOutcomeDecision("My summary")
                .pageNumber("A1")
                .appellantName("Felix Sydney")
                .reasonsForDecision(Arrays.asList("My first reasons", "My second reasons"))
                .anythingElse("Something else").build();

        DlaTemplateContent content = DlaScenario.SCENARIO_NON_DESCRIPTOR.getContent(body);
        Assert.assertEquals("DLA", content.getBenefitTypeInitials());
        Assert.assertEquals("Disability Living Allowance", content.getBenefitTypeNameWithoutInitials());
        Assert.assertNull(content.getRegulationsYear());
    }

}
