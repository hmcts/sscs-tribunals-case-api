package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.gen;

import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.gen.scenarios.GenScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateContent;

public abstract class GenTemplateContent extends WriteFinalDecisionTemplateContent {

    @Override
    protected String getBenefitTypeNameWithoutInitials() {
        return "Generic Benefit Type";
    }

    protected String getRegulationsYear() {
        return null;
    }

    @Override
    protected String getBenefitTypeInitials() {
        return "GEN";
    }

    public abstract GenScenario getScenario();
}
