package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios.EsaScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateContent;

public abstract class EsaTemplateContent extends WriteFinalDecisionTemplateContent {

    public String getAllowedOrRefusedSentence(boolean allowed) {
        return "The appeal is " + (allowed ? "allowed" : "refused") + ".";
    }

    public String getConfirmedOrSetAsideSentence(boolean setAside, String decisionDate) {
        return "The decision made by the Secretary of State on " + decisionDate + " is "
            + (!setAside ? "confirmed." : "set aside.");
    }

    public String getDoesNotHaveLimitedCapabilityForWorkSentence(String appellantName) {
        return appellantName + " does not have limited capability for work and cannot be treated as having limited capability for work.";
    }

    public String getSchedule2InsuffientPointsSentence(Integer points, Boolean regulation29Applies) {
        return "In applying the work capability assessment " + points + (points == 1 ? " point was" : " points were")
            + " scored from the activities and descriptors in Schedule "
            + "2 of the ESA Regulations 2008. This is insufficient to meet the "
            + "threshold for the test." + (regulation29Applies != null
            && !regulation29Applies.booleanValue() ? ""
            : "Regulation 29 of the Employment and Support Allowance (ESA) Regulations 2008 did not apply.");
    }

    public abstract EsaScenario getScenario();
}
