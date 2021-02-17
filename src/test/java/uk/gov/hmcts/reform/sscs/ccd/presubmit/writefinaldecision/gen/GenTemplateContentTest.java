package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.gen;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.gen.scenarios.GenScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class GenTemplateContentTest {

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

        GenTemplateContent content = GenScenario.SCENARIO_NON_DESCRIPTOR.getContent(body);
        Assert.assertEquals("GEN", content.getBenefitTypeInitials());
        Assert.assertEquals("Generic Benefit Type", content.getBenefitTypeNameWithoutInitials());
        Assert.assertNull(content.getRegulationsYear());
    }

}
