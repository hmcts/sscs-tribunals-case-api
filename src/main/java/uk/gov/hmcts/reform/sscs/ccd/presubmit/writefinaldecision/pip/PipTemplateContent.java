package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import java.time.LocalDate;
import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios.PipScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateContent;

public abstract class PipTemplateContent extends WriteFinalDecisionTemplateContent {

    @Override
    protected String getBenefitTypeNameWithoutInitials() {
        return "Personal Independence Payment";
    }

    protected String getRegulationsYear() {
        return null;
    }

    @Override
    protected String getBenefitTypeInitials() {
        return "PIP";
    }

    protected String getIsEntitledDailyLiving(String appellantName) {
        return appellantName + " is entitled to the daily living component at the standard rate from 10/10/2020 for an indefinite period.";
    }

    protected String getIsEntitledMobility(String appellantName) {
        return appellantName + " is entitled to the mobility component at the standard rate from 10/10/2020 for an indefinite period.";
    }

    public String getConfirmedOrSetAsideSentence(boolean setAside, String decisionDate) {
        return "The decision made by the Secretary of State on " + DATEFORMATTER.format(LocalDate.parse(decisionDate)) + " in respect of Personal Independence Payment is "
                + (!setAside ? "confirmed." : "set aside.");
    }

    protected String getLimitedAbilityDailyLiving(String appellantName, Integer points, List<Descriptor> descriptors) {
        if (points == null) {
            points = 0;
        }
        return appellantName + " has limited ability to carry out the activities of daily living set out below. They score " + points + " point" + (points == 1 ? "." : "s.") + (descriptors != null && descriptors.size() > 0 ? " They satisfy the following descriptors:" : "");
    }

    protected String getLimitedAbilityMobility(String appellantName, Integer points, List<Descriptor> descriptors) {
        if (points == null) {
            points = 0;
        }
        return appellantName + " is limited in their ability to mobilise. They score " + points + " point" + (points == 1 ? "." : "s.") + (descriptors != null && descriptors.size() > 0 ? " They satisfy the following descriptors:" : "");
    }

    public abstract PipScenario getScenario();
}
