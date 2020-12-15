package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import java.time.LocalDate;
import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios.PipScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateContent;

public abstract class PipTemplateContent extends WriteFinalDecisionTemplateContent {

    @Override
    protected String getBenefitTypeNameWithoutInitials() {
        return "Employment and Support Allowance";
    }

    protected String getRegulationsYear() {
        return "2008";
    }

    @Override
    protected String getBenefitTypeInitials() {
        return "ESA";
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

    public String getDoesNotHaveLimitedCapabilityForWorkSentence(String appellantName) {
        return appellantName + " does not have limited capability for work and cannot be treated as having limited capability for work.";
    }

    public String getDoesNotHaveLimitedCapabilityForWorkNoSchedule3Sentence(String appellantName) {
        return appellantName + " does not have limited capability for work-related activity because no descriptor from Schedule 3 of the " + getUsageDependentBenefitTypeRegulationsString() + " applied. Regulation 35 did not apply.";
    }

    public String getDoesHaveLimitedCapabilityForWorkSentence(String appellantName, boolean isTreatedLimitedCapability, boolean includeWorkRelatedActivities, boolean isWorkRelatedActivitiesLimited) {
        return appellantName + (isTreatedLimitedCapability ? " is to be treated as having" : " has") + " limited capability for work" + (includeWorkRelatedActivities ? " and " + (isWorkRelatedActivitiesLimited ? "has limited capability " : "") + "for work-related activity." : ".");
    }

    public String getDoesHaveLimitedCapabilityForWorkSentence(String appellantName, boolean isTreatedLimitedCapability, boolean includeWorkRelatedActivities, boolean isWorkRelatedActivitiesLimited, boolean isWorkRelatedActivitiesToBeTreatedLimitedCapability) {
        return (appellantName + (isTreatedLimitedCapability ? " is to be treated as having" : " has") + " limited capability for work" + (includeWorkRelatedActivities ? " and " + (
            !isWorkRelatedActivitiesLimited ? "" : (isWorkRelatedActivitiesToBeTreatedLimitedCapability ? "is to be treated as having limited capability " : "has limited capability "))
            + "for work-related activity." : "."));
    }

    public String getLimitedCapabilityForWorkRelatedSentence(String appellantName, boolean isTreatedLimitedCapability) {
        return appellantName + (isTreatedLimitedCapability ? " is to be treated as having" : " has") + " limited capability for work-related activity.";
    }

    public String getContinuesToHaveWorkRelatedSentenceButNotLimitedWorkRelatedActivity(String appellantName) {
        return appellantName + " continues to have limited capability for work but does not have limited capability for "
                + "work-related activity.";
    }

    public String getSecretaryOfStateAcceptsHasLimitedCapabilityForWorkSentence(String appellantName, boolean work) {
        return "The Secretary of State has accepted that " + appellantName + " has limited capability for "
                + (work ? "work." : "work-related activity.") + " This was not in issue.";
    }

    public String getHasLimitedCapabilityForWorkNoSchedule3SentenceReg35Applies() {
        return "No activity or descriptor from Schedule 3 of the " + getUsageDependentBenefitTypeRegulationsString() + " was satisfied but regulation 35 of the " +  getUsageDependentBenefitTypeRegulationsString() + " applied.";
    }

    public String getSchedule2PointsSentence(Integer points, Boolean isSufficient, List<Descriptor> esaSchedule2Descriptors) {
        String madeUpAsFollowsSuffix = esaSchedule2Descriptors == null || esaSchedule2Descriptors.isEmpty() ? "." : " made up as follows:";
        return "In applying the Work Capability Assessment " + points + (points == 1 ? " point was" : " points were")
            + " scored from the activities and descriptors in Schedule "
            + "2 of the " + getUsageDependentBenefitTypeRegulationsString() + (isSufficient != null && isSufficient.booleanValue() ? madeUpAsFollowsSuffix
            : ". This is insufficient to meet the "
            + "threshold for the test. Regulation 29 of the " + getUsageDependentBenefitTypeRegulationsString() + " did not apply.");
    }

    public String getInsufficientPointsSentenceRegulation29Applied() {
        return "This is because insufficient points were scored to meet the threshold for the Work Capability Assessment, "
                + "but regulation 29 of the " + getUsageDependentBenefitTypeRegulationsString() + " applied.";
    }

    public String getInsufficientPointsSentenceRegulation29AndRegulation35Applied() {
        return "This is because insufficient points were scored under Schedule 2 of the " + getUsageDependentBenefitTypeRegulationsString() +  " to meet the threshold for the Work Capability Assessment "
                + "and none of the Schedule 3 activities or descriptors were satisfied.";
    }

    public String getInsufficientPointsSentence() {
        return "This is because insufficient points were scored under Schedule 2 of the " + getUsageDependentBenefitTypeRegulationsString() + " to meet the threshold for the Work Capability Assessment.";
    }

    public String getRegulation29And35DiseaseOrDisablementSentence(boolean isRegulation29Applied, boolean isRegulation35Applied) {
        return "The tribunal applied regulation" + (isRegulation29Applied && isRegulation35Applied ? "s" : "")
                + (isRegulation29Applied ? " 29 " : "")
                + (isRegulation29Applied && isRegulation35Applied ? "and" : "")
                + (isRegulation35Applied ? " 35 " : "")
            + "because there would be a substantial risk to the mental or physical health of any person if the appellant were found not to have limited "
            + "capability for work"
            + (isRegulation35Applied ? " and for work-related activity." : ".");
    }

    public String getSchedule3AppliesParagraph(List<Descriptor> descriptors) {
        if (descriptors != null && descriptors.size() == 1) {
            return "The following activity and descriptor from Schedule 3 of the " + getUsageDependentBenefitTypeRegulationsString() + " applied:";
        } else {
            return "The following activities and descriptors from Schedule 3 of the " + getUsageDependentBenefitTypeRegulationsString() + " applied:";
        }
    }

    public String getNoDescriptorFromSchedule3Regulation35NotApplied() {
        return "This is because no descriptor from Schedule 3 of the " + getUsageDependentBenefitTypeRegulationsString() + " applied. Regulation 35 did not apply.";
    }

    public abstract PipScenario getScenario();
}
