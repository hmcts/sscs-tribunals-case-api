package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import java.time.format.DateTimeFormatter;
import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios.UcScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateContent;

public abstract class UcTemplateContent extends WriteFinalDecisionTemplateContent {

    protected static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public abstract UcScenario getScenario();

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
}
