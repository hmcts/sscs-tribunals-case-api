package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios.UcScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateContent;

public abstract class UcTemplateContent extends WriteFinalDecisionTemplateContent {

    protected String getBenefitTypeNameWithoutInitials() {
        return "Universal Credit";
    }

    @Override
    protected String getBenefitTypeInitials() {
        return "UC";
    }

    protected String getRegulationsYear() {
        return "2013";
    }

    public abstract UcScenario getScenario();

    public String getDoesNotHaveLimitedCapabilityForWorkSentence(String appellantName) {
        return appellantName + " does not have limited capability for work and cannot be treated as having limited capability for work. The matter is now remitted to the Secretary of State to make a final decision upon entitlement to " + getUsageDependentBenefitTypeString() + ".";
    }

    public String getDoesNotHaveLimitedCapabilityForWorkNoSchedule7Sentence(String appellantName) {
        return appellantName + " does not have limited capability for work-related activity because no descriptor from Schedule 7 of the " + getUsageDependentBenefitTypeRegulationsString() + " applied. Schedule 9, paragraph 4 did not apply.";
    }

    public String getDoesHaveLimitedCapabilityForWorkSentence(String appellantName, boolean isTreatedLimitedCapability, boolean includeWorkRelatedActivities, boolean isWorkRelatedActivitiesLimited, boolean isWorkRelatedActivitiesToBeTreatedLimitedCapability) {
        return (appellantName + (isTreatedLimitedCapability ? " is to be treated as having" : " has") + " limited capability for work" + (includeWorkRelatedActivities ? " and " + (!isWorkRelatedActivitiesLimited ? "" : (isWorkRelatedActivitiesToBeTreatedLimitedCapability ? "is to be treated as having limited capability " : "has limited capability ")) + "for work-related activity." : "."))
            + " The matter is now remitted to the Secretary of State to make a final decision upon entitlement to " + getUsageDependentBenefitTypeString() + ".";
    }

    public String getLimitedCapabilityForWorkRelatedSentence(String appellantName, boolean isTreatedLimitedCapability) {
        return appellantName + (isTreatedLimitedCapability ? " is to be treated as having" : " has") + " limited capability for work-related activity."
            + " The matter is now remitted to the Secretary of State to make a final decision upon entitlement to " + getUsageDependentBenefitTypeString() + ".";
    }

    public String getSchedule7AppliesParagraph(List<Descriptor> descriptors) {
        if (descriptors != null && descriptors.size() == 1) {
            return "The following activity and descriptor from Schedule 7 of the " + getUsageDependentBenefitTypeRegulationsString() + " applied: ";
        } else {
            return "The following activities and descriptors from Schedule 7 of the " + getUsageDependentBenefitTypeRegulationsString() + " applied: ";
        }
    }

    public String getSchedule6PointsSentence(Integer points, Boolean isSufficient, List<Descriptor> ucSchedule6Descriptors) {
        return "In applying the Work Capability Assessment " + points + (points == 1 ? " point was" : " points were")
            + " scored from the activities and descriptors in Schedule "
            + "6 of the " + getUsageDependentBenefitTypeRegulationsString() + getSchedule6PointsSentenceMadeUpAsFollowsSuffix(isSufficient, ucSchedule6Descriptors);
    }

    private String getSchedule6PointsSentenceMadeUpAsFollowsSuffix(Boolean isSufficient, List<Descriptor> ucSchedule6Descriptors) {
        return isSufficient != null && isSufficient
            ? ucSchedule6Descriptors == null || ucSchedule6Descriptors.isEmpty() ? "." : " made up as follows:"
            : ". This is insufficient to meet the "
                + "threshold for the test. Schedule 8, paragraph 4 of the " + getUsageDependentBenefitTypeRegulationsString() + " did not apply.";
    }

    public String getInsufficientPointsSentenceSchedule8Paragraph4Applied() {
        return "This is because insufficient points were scored to meet the threshold for the Work Capability Assessment, "
            + "but Schedule 8, paragraph 4 of the " + getUsageDependentBenefitTypeRegulationsString() + " applied.";
    }

    public String getInsufficientPointsSentenceNoSchedule8Paragraph4Sentence() {
        return "This is because insufficient points were scored under Schedule 6 of the " + getUsageDependentBenefitTypeRegulationsString() + " to meet the threshold for the Work Capability Assessment and none of the Schedule 7 activities or descriptors were satisfied.";
    }

    public String getInsufficientPointsSentenceNoSchedule8Paragraph4SentenceAndNoSchedule7Sentence() {
        return "This is because insufficient points were scored under Schedule 6 of the " + getUsageDependentBenefitTypeRegulationsString() + " to meet the threshold for the Work Capability Assessment.";
    }

    public String getSchedule8Paragraph4AndSchedule9Paragraph4DiseaseOrDisablementSentence(boolean isSchedule8Paragraph4Applied, boolean isSchedule9Paragraph4Applied) {
        return "The tribunal applied"
            + (isSchedule8Paragraph4Applied ? " Schedule 8, paragraph 4" : "")
            + (isSchedule8Paragraph4Applied && isSchedule9Paragraph4Applied ? " and" : "")
            + (isSchedule9Paragraph4Applied ? " Schedule 9, paragraph 4" : "")
            + " because there would be a substantial risk to the mental or physical health of any person if the appellant were found not to have limited capability"
            + (isSchedule8Paragraph4Applied ? " for work" : "")
            + (isSchedule8Paragraph4Applied && isSchedule9Paragraph4Applied ? " and" : "")
            + (isSchedule9Paragraph4Applied ? " for work-related activity." : ".");
    }

    public String getNoSchedule7SentenceSchedule9Paragraph4Applies() {
        return "No activity or descriptor from Schedule 7 of the " + getUsageDependentBenefitTypeRegulationsString() + " was satisfied but Schedule 9, paragraph 4 of the " + getUsageDependentBenefitTypeRegulationsString() + " applied.";
    }

    public String getContinuesToHaveWorkRelatedSentenceButNotLimitedWorkRelatedActivityOnly(String appellantName) {
        return appellantName + " continues to have limited capability for work but does not have limited capability for "
                + "work-related activity and cannot be treated as having limited capability for work-related activity.";
    }

    public String getNoDescriptorFromSchedule7Schedule9NotApplied() {
        return "This is because no descriptor from Schedule 7 of the " + getUsageDependentBenefitTypeRegulationsString() + " applied. Schedule 9, paragraph 4 did not apply.";
    }

    public String getSecretaryOfStateAcceptsHasLimitedCapabilityForWorkSentence(String appellantName, boolean work) {
        return "The Secretary of State has accepted that " + appellantName + " has limited capability for "
                + (work ? "work." : "work-related activity.") + " This was not in issue.";
    }
}
