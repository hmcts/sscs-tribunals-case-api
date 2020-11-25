package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;


import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios.UcScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateContent;

public abstract class UcTemplateContent extends WriteFinalDecisionTemplateContent {

    public abstract UcScenario getScenario();

}
