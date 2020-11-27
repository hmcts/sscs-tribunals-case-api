package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios.UcTemplateComponentId;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.Paragraph;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

public abstract class UcNewTemplateContent extends UcTemplateContent {

    public String getDoesHaveLimitedCapabilityForWorkSentence(String appellantName) {
        return appellantName + " has limited capability for work. The matter is now remitted to the Secretary of State to make a final decision upon entitlement to UC.";
    }

    public String getSchedule7AppliesParagraph(List<Descriptor> descriptors) {
        if (descriptors != null && descriptors.size() == 1) {
            return "The following activity and descriptor from Schedule 7 of the UC Regulations 2013 applied: ";
        } else {
            return "The following activities and descriptors from Schedule 7 of the UC Regulations 2013 applied: ";
        }
    }

    public List<String> getHearingTypeSentences(String appellantName, String bundlePage, String hearingType, boolean appellantAttended, boolean presentingOfficerAttened) {
        if (equalsIgnoreCase("paper", hearingType)) {
            return asList("No party has objected to the matter being decided without a hearing.", "Having considered the appeal bundle to page " + bundlePage + " and the requirements of rules 2 and 27 of the Tribunal Procedure (First-tier Tribunal) (Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that it is able to decide the case in this way.");
        } else  {
            return getFaceToFaceTelephoneVideoHearingTypeSentences(hearingType, appellantName, bundlePage, appellantAttended, presentingOfficerAttened);
        }
    }

    public List<String> getFaceToFaceTelephoneVideoHearingTypeSentences(String hearingType, String appellantName, String bundlePage,
        boolean appellantAttended, boolean presentingOfifficerAttened) {
        if (appellantAttended) {
            if (equalsIgnoreCase("faceToFace", hearingType)) {
                return singletonList("This has been an oral (face to face) hearing. "
                        + getAppellantAttended(hearingType, appellantName, presentingOfifficerAttened, bundlePage));
            } else {
                return singletonList("This has been a remote hearing in the form of a " + hearingType + " hearing. "
                    + getAppellantAttended(hearingType, appellantName, presentingOfifficerAttened, bundlePage));
            }
        } else {
            if (equalsIgnoreCase("faceToFace", hearingType)) {
                return asList(appellantName + " requested an oral hearing but did not attend today. "
                    + (presentingOfifficerAttened ? "A " : "No ") + "Presenting Officer attended on behalf of the Respondent.",
                    getConsideredParagraph(bundlePage, appellantName));
            } else {
                return asList("This has been a remote hearing in the form of a " + hearingType + " hearing. " + appellantName + " did not attend the hearing today. "
                    + (presentingOfifficerAttened ? "A" : "No") + " Presenting Officer attended on behalf of the Respondent.",
                    getConsideredParagraph(bundlePage, appellantName));
            }
        }
    }

    public void addHearingType(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        for (String hearingTypeSentence : getHearingTypeSentences(writeFinalDecisionTemplateBody.getAppellantName(), writeFinalDecisionTemplateBody.getPageNumber(),
            writeFinalDecisionTemplateBody.getHearingType(), writeFinalDecisionTemplateBody.isAttendedHearing(), writeFinalDecisionTemplateBody.isPresentingOfficerAttended())) {
            addComponent(new Paragraph(UcTemplateComponentId.HEARING_TYPE.name(), hearingTypeSentence));
        }
    }

    public String getSchedule6PointsSentence(Integer points, Boolean isSufficient, List<Descriptor> ucSchedule6Descriptors) {
        String madeUpAsFollowsSuffix = ucSchedule6Descriptors == null || ucSchedule6Descriptors.isEmpty() ? "." : " made up as follows:";
        return "In applying the Work Capability Assessment " + points + (points == 1 ? " point was" : " points were")
            + " scored from the activities and descriptors in Schedule "
            + "6 of the UC Regulations 2013" + madeUpAsFollowsSuffix;
    }

    public String getInsufficientPointsSentenceSchedule8Paragraph4Applied() {
        return "This is because insufficient points were scored to meet the threshold for the Work Capability Assessment, "
            + "but Schedule 8, paragraph 4 of the UC Regulations 2013 applied.";
    }

    public String getSchedule8Paragraph4AndSchedule9Paragraph4DiseaseOrDisablementSentence(boolean isSchedule8Paragraph4Applied, boolean isSchedule9Paragraph4Applied) {
        return "The tribunal applied Schedule" + (isSchedule8Paragraph4Applied && isSchedule9Paragraph4Applied ? "s" : "")
            + (isSchedule8Paragraph4Applied ? " 8, paragraph 4 " : "")
            + (isSchedule8Paragraph4Applied && isSchedule9Paragraph4Applied ? "and" : "")
            + (isSchedule9Paragraph4Applied ? " 9, paragraph 4" : "")
            + "because there would be a substantial risk to the mental or physical health of any person if the appellant were found not to have limited "
            + "capability for work"
            + (isSchedule9Paragraph4Applied ? " and for work-related activity." : ".");
    }

    public String getNoSchedule7Sentence() {
        return "No descriptor from Schedule 7 of the UC Regulations 2013 was satisfied.";
    }

    public String getNoSchedule7SentenceSchedule9Paragraph4Applies() {
        return "No descriptor from Schedule 7 of the UC Regulations 2013 was satisfied but Schedule 9, paragraph 4 of the UC Regulations 2013 applied.";
    }

}
