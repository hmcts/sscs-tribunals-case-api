package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios;

import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.PipTemplateContent;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public class Scenario1Content extends PipTemplateContent {

    public Scenario1Content(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        // Default constructor
    }

    @Override
    public PipScenario getScenario() {
        return PipScenario.SCENARIO_1;
    }
}
