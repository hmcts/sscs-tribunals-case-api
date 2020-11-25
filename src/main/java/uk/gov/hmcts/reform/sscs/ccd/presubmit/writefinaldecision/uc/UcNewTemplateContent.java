package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.startsWith;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios.UcScenario;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios.UcTemplateComponentId;
import uk.gov.hmcts.reform.sscs.model.docassembly.Paragraph;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateBody;

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

    public String getInsufficientPointsSentenceSchedule8Paragraph4Applied() {
        return "This is because insufficient points were scored to meet the threshold for the Work Capability Assessment, "
            + "but Schedule 8, paragraph 4 of the UC Regulations 2013 applied.";
    }

    public void addReasonsIfPresent(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        if (writeFinalDecisionTemplateBody.getReasonsForDecision() != null) {
            for (String reason : writeFinalDecisionTemplateBody.getReasonsForDecision()) {
                addComponent(new Paragraph(UcTemplateComponentId.REASON.name(), reason));
            }
        }
    }

    public void addAnythingElseIfPresent(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        if (writeFinalDecisionTemplateBody.getAnythingElse() != null) {
            addComponent(new Paragraph(UcTemplateComponentId.ANYTHING_ELSE.name(), writeFinalDecisionTemplateBody.getAnythingElse()));
        }
    }

    public void addHearingType(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        addComponent(new Paragraph(UcTemplateComponentId.HEARING_TYPE.name(), getHearingTypeSentence(writeFinalDecisionTemplateBody.getAppellantName(), writeFinalDecisionTemplateBody.getPageNumber(),
            writeFinalDecisionTemplateBody.getHearingType(), writeFinalDecisionTemplateBody.isAttendedHearing(), writeFinalDecisionTemplateBody.isPresentingOfficerAttended())));
    }

    public void addRecommendationIfPresent(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        if (writeFinalDecisionTemplateBody.getDwpReassessTheAward() != null) {
            addComponent(new Paragraph(UcTemplateComponentId.RECOMMENDATION.name(),
                getRecommendationSentence(writeFinalDecisionTemplateBody.getDwpReassessTheAward(), writeFinalDecisionTemplateBody.getAppellantName())));
        }
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

    public String getRecommendationSentence(String code, String appellantName) {
        final String firstSentence = "Any recommendation given below does not form part of the Tribunal's decision and is not binding on the Secretary of State.";
        final String secondSentence;
        if ("noRecommendation".equals(code)) {
            secondSentence = format("The Tribunal makes no recommendation as to when the Department should reassess %s.", appellantName);
        } else if ("doNotReassess".equals(code)) {
            secondSentence = "In view of the degree of disability found by the Tribunal, and unless the regulations change, the Tribunal would recommend that the appellant is not re-assessed.";
        } else if (startsWith(code, "doNotReassess")) {
            secondSentence = format("The Tribunal recommends that the Department does not reassess %s within %s months from today's date.", appellantName, removeStart(code, "doNotReassess"));
        } else if (startsWith(code, "reassess")) {
            secondSentence = format("The Tribunal recommends that the Department reassesses %s within %s months from today's date.", appellantName, removeStart(code, "reassess"));
        } else {
            throw new IllegalArgumentException(format("Error: Unknown DWP reassess award code, please check if this code '%s' is in the FixedLists in the CCD file.", code));
        }
        // Placeholder for SSCS-8308 (Ryan)
        return format("%s %s", firstSentence, secondSentence);
    }

    public String getHearingTypeSentence(String appellantName, String bundlePage, String hearingType, boolean appellantAttended, boolean presentingOfifficerAttened) {
        if (StringUtils.equalsIgnoreCase("paper", hearingType)) {
            return "No party has objected to the matter being decided without a hearing. Having considered the appeal bundle to page " + bundlePage + " and the requirements of rules 2 and 27 of the Tribunal Procedure (First-tier Tribunal) (Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that it is able to decide the case in this way.";
        } else  {
            return getFaceToFaceTelephoneVideoHearingTypeSentence(hearingType, appellantName, bundlePage, appellantAttended, presentingOfifficerAttened);
        }
    }

    private String getConsideredParagraph(String bundlePage, String appellantName) {
        return "Having considered the appeal bundle to page " + bundlePage + " and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify " + appellantName + " of the hearing and that it is in the interests of justice to proceed today. ";
    }

    private String getAppellantAttended(String hearingType, String appellantName, boolean presentingOfifficerAttened, String bundlePage) {
        if (StringUtils.equalsIgnoreCase("faceToFace", hearingType)) {
            return appellantName + " attended the hearing today and the Tribunal considered the appeal bundle to page " + bundlePage + ". "
                + (presentingOfifficerAttened ? "A" : "No") + " Presenting Officer attended on behalf of the Respondent.";
        } else {
            return appellantName + " attended and the Tribunal considered the appeal bundle to page " + bundlePage + ". "
                + (presentingOfifficerAttened ? "A" : "No") + " Presenting Officer attended on behalf of the Respondent.";
        }

    }

    public String getFaceToFaceTelephoneVideoHearingTypeSentence(String hearingType, String appellantName, String bundlePage,
        boolean appellantAttended, boolean presentingOfifficerAttened) {
        if (appellantAttended) {
            if (StringUtils.equalsIgnoreCase("faceToFace", hearingType)) {
                return "This has been an oral (face to face) hearing. "
                    + getAppellantAttended(hearingType, appellantName, presentingOfifficerAttened, bundlePage);
            } else {
                return "This has been a remote hearing in the form of a " + hearingType + " hearing. "
                    + getAppellantAttended(hearingType, appellantName, presentingOfifficerAttened, bundlePage);
            }
        } else {
            if (StringUtils.equalsIgnoreCase("faceToFace", hearingType)) {
                return appellantName + " requested an oral hearing but did not attend today. "
                    + (presentingOfifficerAttened ? "A " : "No ") + "Presenting Officer attended on behalf of the Respondent. "
                    + "\n"
                    + getConsideredParagraph(bundlePage, appellantName);
            } else {
                return "This has been a remote hearing in the form of a " + hearingType + " hearing. " + appellantName + " did not attend the hearing today. "
                    + (presentingOfifficerAttened ? "A" : "No") + " Presenting Officer attended on behalf of the Respondent.\n"
                    + "\n"
                    + getConsideredParagraph(bundlePage, appellantName);
            }
        }
    }


}
