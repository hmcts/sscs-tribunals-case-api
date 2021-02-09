package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.dla;

import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.dla.scenarios.DlaScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateContent;

public abstract class DlaTemplateContent extends WriteFinalDecisionTemplateContent {

    @Override
    protected String getBenefitTypeNameWithoutInitials() {
        return "Disability Living Allowance";
    }

    protected String getRegulationsYear() {
        return null;
    }

    @Override
    protected String getBenefitTypeInitials() {
        return "DLA";
    }

    public abstract DlaScenario getScenario();
}
