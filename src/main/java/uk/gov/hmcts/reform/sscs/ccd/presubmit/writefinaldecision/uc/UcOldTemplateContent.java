package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios.UcScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;

public abstract class UcOldTemplateContent extends UcTemplateContent {

    protected static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public String getAllowedOrRefusedSentence(boolean allowed) {
        return "The appeal is " + (allowed ? "allowed" : "refused") + ".";
    }

    public String getConfirmedOrSetAsideSentence(boolean setAside, String decisionDate) {
        return "The decision made by the Secretary of State on " + DATE_FORMATTER.format(LocalDate.parse(decisionDate)) + " is "
            + (!setAside ? "confirmed." : "set aside.");
    }

    public String getDoesNotHaveLimitedCapabilityForWorkSentence(String appellantName) {
        return appellantName + " does not have limited capability for work and cannot be treated as having limited capability for work.";
    }

    public String getDoesNotHaveLimitedCapabilityForWorkNoSchedule7Sentence(String appellantName) {
        return appellantName + " does not have limited capability for work-related activity because no descriptor from Schedule 3 applied.  Regulation 35 did not apply.";
    }

    public String getDoesHaveLimitedCapabilityForWorkSentence(String appellantName, boolean isTreatedLimitedCapability, boolean includeWorkRelatedActivities, boolean isWorkRelatedActivitiesLimited) {
        return appellantName + (isTreatedLimitedCapability ? " is to be treated as having" : " has") + " limited capability for work" + (includeWorkRelatedActivities ? " and " + (isWorkRelatedActivitiesLimited ? "has limited capability " : "") + "for work-related activity." : ".");
    }

    public String getLimitedCapabilityForWorkRelatedSentence(String appellantName, boolean isTreatedLimitedCapability) {
        return appellantName + (isTreatedLimitedCapability ? " is to be treated as having" : " has") + " limited capability for work-related activity.";
    }

    public String getContinuesToHaveWorkRelatedSentenceButNotLimitedWorkRelatedActivity(String appellantName) {
        return appellantName + " continues to have limited capability for work but does not have limited capability for "
                + "work-related activity. This is because no descriptor from Schedule 3 of the Employment and "
                + "Support Allowance (ESA) Regulations 2008 applied. Regulation 35 did not apply. The Secretary of State "
                + "has accepted that " + appellantName + " has limited capability for work. This was not in issue.";
    }

    public String getSecretaryOfStateAcceptsHasLimitedCapabilityForWorkSentence(String appellantName, boolean work) {
        return "The Secretary of State has accepted that " + appellantName + " has limited capability for "
                + (work ? "work." : "work related activity.") + " This was not an issue.";
    }

    public String getHasLimitedCapabilityForWorkNoSchedule7SentenceSchedule9Paragraph4Applies() {
        return "No descriptor from Schedule 3 of the Employment and Support Allowance (ESA) Regulations 2008 was satisfied but regulation 35 applied.";
    }


    public String getSchedule6PointsSentence(Integer points, Boolean isSufficient) {
        return "In applying the work capability assessment " + points + (points == 1 ? " point was" : " points were")
            + " scored from the activities and descriptors in Schedule "
            + "2 of the ESA Regulations 2008" + (isSufficient != null && isSufficient.booleanValue() ? " made up as follows:"
            : ". This is insufficient to meet the "
            + "threshold for the test. Regulation 29 of the Employment and Support Allowance (ESA) Regulations 2008 did not apply.");
    }

    public String getInsufficientPointsSentenceSchedule8Paragraph4Applied() {
        return "This is because insufficient points were scored to meet the threshold for the work capability assessment, "
                + "but regulation 29 of the Employment and Support Allowance (ESA) Regulations 2008 applied.";
    }

    public String getInsufficientPointsSentenceSchedule8Paragraph4AndSchedule9Paragraph4Applied() {
        return "This is because insufficient points were scored to meet the threshold for the work capability assessment "
                + "and none of the Schedule 3 activities and descriptors were satisfied, but the tribunal applied regulations 29 and 35 of the Employment and Support Allowance Regulations (ESA) 2008.";
    }

    public String getInsufficientPointsSentenceSchedule8Paragraph4AndSchedule7Applied() {
        return "This is because insufficient points were scored under Schedule 2 of the Employment and Support Allowance (ESA) Regulations 2008 to meet the threshold for the Work Capability Assessment, but the tribunal applied regulation 29.";
    }

    public String getSchedule8Paragraph4AndSchedule9Paragraph4DiseaseOrDisablementSentence(boolean isSchedule8Paragraph4Applied, boolean isSchedule9Paragraph4Applied) {
        return "The tribunal applied regulation" + (isSchedule8Paragraph4Applied && isSchedule9Paragraph4Applied ? "s" : "")
                + (isSchedule8Paragraph4Applied ? " 29 " : "")
                + (isSchedule8Paragraph4Applied && isSchedule9Paragraph4Applied ? "and" : "")
                + (isSchedule9Paragraph4Applied ? " 35 " : "")
            + "because there would be a substantial risk to the mental or physical health of any person if the appellant were found not to have limited "
            + "capability for work"
            + (isSchedule9Paragraph4Applied ? " and for work-related activity." : ".");
    }

    public String getSchedule7AppliesParagraph(List<Descriptor> descriptors) {
        if (descriptors != null && descriptors.size() == 1) {
            return "The following activity and descriptor from Schedule 3 applied:";
        } else {
            return "The following activities and descriptors from Schedule 3 applied:";
        }
    }

    public abstract UcScenario getScenario();
}
