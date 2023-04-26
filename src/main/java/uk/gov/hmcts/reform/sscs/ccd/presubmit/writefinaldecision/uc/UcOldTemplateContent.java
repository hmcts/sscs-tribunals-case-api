package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;

/**
 * This is a temporary class to support the un-migrated scenarios, containing content that has initially been ported
 * over from ESA.  As we migrate each scenario over, the new content should be added to UcNewTemplateContent.
 * Once we have move over all the scenarios,  this class can be removed.
 */
public abstract class UcOldTemplateContent extends UcTemplateContent {

    public String getDoesNotHaveLimitedCapabilityForWorkSentence(String appellantName) {
        return appellantName + " does not have limited capability for work and cannot be treated as having limited capability for work.";
    }

    public String getDoesHaveLimitedCapabilityForWorkSentence(String appellantName, boolean isTreatedLimitedCapability, boolean includeWorkRelatedActivities, boolean isWorkRelatedActivitiesLimited) {
        return appellantName + (isTreatedLimitedCapability ? " is to be treated as having" : " has") + " limited capability for work" + (includeWorkRelatedActivities ? " and " + (isWorkRelatedActivitiesLimited ? "has limited capability " : "") + "for work-related activity." : ".");
    }

    public String getLimitedCapabilityForWorkRelatedSentence(String appellantName, boolean isTreatedLimitedCapability) {
        return appellantName + (isTreatedLimitedCapability ? " is to be treated as having" : " has") + " limited capability for work-related activity.";
    }

    public String getSecretaryOfStateAcceptsHasLimitedCapabilityForWorkSentence(String appellantName, boolean work) {
        return "The Secretary of State has accepted that " + appellantName + " has limited capability for "
                + (work ? "work." : "work related activity.") + " This was not in issue.";
    }

    public String getSchedule6PointsSentence(Integer points, Boolean isSufficient) {
        return "In applying the work capability assessment " + points + (points == 1 ? " point was" : " points were")
            + " scored from the activities and descriptors in Schedule "
            + "2 of the ESA Regulations 2008" + (isSufficient != null && isSufficient.booleanValue() ? " made up as follows:"
            : ". This is insufficient to meet the "
            + "threshold for the test. Regulation 29 of the Employment and Support Allowance (ESA) Regulations 2008 did not apply.");
    }

    public String getInsufficientPointsSentenceSchedule8Paragraph4AndSchedule9Paragraph4Applied() {
        return "This is because insufficient points were scored to meet the threshold for the work capability assessment "
                + "and none of the Schedule 3 activities and descriptors were satisfied, but the tribunal applied regulations 29 and 35 of the Employment and Support Allowance Regulations (ESA) 2008.";
    }

    public String getInsufficientPointsSentenceSchedule8Paragraph4AndSchedule7Applied() {
        return "This is because insufficient points were scored under Schedule 2 of the Employment and Support Allowance (ESA) Regulations 2008 to meet the threshold for the Work Capability Assessment, but the tribunal applied regulation 29.";
    }

    public String getSchedule8Paragraph4AndSchedule9Paragraph4DiseaseOrDisablementSentence(boolean isSchedule9Paragraph4Applied) {
        return "The tribunal applied regulation" + (isSchedule9Paragraph4Applied ? "s" : "")
                + " 29 "
                + (isSchedule9Paragraph4Applied ? "and" : "")
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

    public String getHearingTypeSentence(String appellantName, String bundlePage, String hearingType, boolean appellantAttended, boolean presentingOfifficerAttened) {
        return getFaceToFaceTelephoneVideoHearingTypeSentence(hearingType, appellantName, bundlePage, appellantAttended, presentingOfifficerAttened);
    }

    public String getFaceToFaceTelephoneVideoHearingTypeSentence(String hearingType, String appellantName, String bundlePage,
        boolean appellantAttended, boolean presentingOfifficerAttened) {
        if (appellantAttended) {
            if (StringUtils.equalsIgnoreCase("faceToFace", hearingType)) {
                return "This has been an oral (face to face) hearing. "
                    + getAppellantAndOtherPartyAttended(hearingType, appellantName, presentingOfifficerAttened, bundlePage, null);
            } else {
                return "This has been a remote hearing in the form of a " + hearingType + " hearing. "
                    + getAppellantAndOtherPartyAttended(hearingType, appellantName, presentingOfifficerAttened, bundlePage, null);
            }
        } else {
            return "This has been a remote hearing in the form of a " + hearingType + " hearing. " + appellantName + " did not attend the hearing today. "
                + (presentingOfifficerAttened ? "A" : "No") + " Presenting Officer attended on behalf of the Respondent.\n"
                + "\n"
                + getConsideredParagraph(bundlePage, appellantName);

        }
    }
}
