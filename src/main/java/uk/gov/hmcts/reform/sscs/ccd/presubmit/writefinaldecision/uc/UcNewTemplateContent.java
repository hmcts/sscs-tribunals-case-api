package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios.UcScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;

public abstract class UcNewTemplateContent extends UcTemplateContent {

    protected static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public abstract UcScenario getScenario();

    public String getAllowedOrRefusedSentence(boolean allowed) {
        return "The appeal is " + (allowed ? "allowed" : "refused") + ".";
    }

    public String getConfirmedOrSetAsideSentence(boolean setAside, String decisionDate) {
        return "The decision made by the Secretary of State on " + DATE_FORMATTER.format(LocalDate.parse(decisionDate)) + " is "
            + (!setAside ? "confirmed." : "set aside.");
    }

    public String getDoesHaveLimitedCapabilityForWorkSentence(String appellantName) {
        return appellantName + " has limited capability for work. The matter is now remitted to the Secretary of State to make a final decision upon entitlement to UC.";
    }

    public String getSchedule7AppliesParagraph(List<Descriptor> descriptors) {
        if (descriptors != null && descriptors.size() == 1) {
            return "The following activity and descriptor from Schedule 3 applied:";
        } else {
            return "The following activities and descriptors from Schedule 3 applied:";
        }
    }

    public String getSchedule6PointsSentence(Integer points, Boolean isSufficient) {
        return "In applying the Work Capability Assessment " + points + (points == 1 ? " point was" : " points were")
            + " scored from the activities and descriptors in Schedule "
            + "6 of the UC Regulations 2013 made up as follows:"; // + (isSufficient != null && isSufficient.booleanValue() ? " made up as follows:"
            // : ". This is insufficient to meet the "
            //   + "threshold for the test. Regulation 29 of the Employment and Support Allowance (ESA) Regulations 2008 did not apply.");
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
