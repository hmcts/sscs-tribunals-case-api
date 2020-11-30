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
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateContent;

public abstract class UcTemplateContent extends WriteFinalDecisionTemplateContent {

    protected static DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public abstract UcScenario getScenario();

    public void addReasonsIfPresent(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        if (writeFinalDecisionTemplateBody.getReasonsForDecision() != null) {
            for (String reason : writeFinalDecisionTemplateBody.getReasonsForDecision()) {
                addComponent(new Paragraph(UcTemplateComponentId.REASON.name(), reason));
            }
        }
    }

    public String getAllowedOrRefusedSentence(boolean allowed) {
        return "The appeal is " + (allowed ? "allowed" : "refused") + ".";
    }

    public String getConfirmedOrSetAsideSentence(boolean setAside, String decisionDate) {
        return "The decision made by the Secretary of State on " + DATE_FORMATTER.format(LocalDate.parse(decisionDate)) + " is "
            + (!setAside ? "confirmed." : "set aside.");
    }

    public void addAnythingElseIfPresent(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        if (StringUtils.isNotBlank(writeFinalDecisionTemplateBody.getAnythingElse())) {
            addComponent(new Paragraph(UcTemplateComponentId.ANYTHING_ELSE.name(), writeFinalDecisionTemplateBody.getAnythingElse()));
        }
    }

    public void addRecommendationIfPresent(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        if (writeFinalDecisionTemplateBody.getDwpReassessTheAward() != null) {
            addComponent(new Paragraph(UcTemplateComponentId.RECOMMENDATION.name(),
                getRecommendationSentence(writeFinalDecisionTemplateBody.getDwpReassessTheAward(), writeFinalDecisionTemplateBody.getAppellantName())));
        }
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

    protected String getConsideredParagraph(String bundlePage, String appellantName) {
        return "Having considered the appeal bundle to page " + bundlePage + " and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify " + appellantName + " of the hearing and that it is in the interests of justice to proceed today. ";
    }

    protected String getAppellantAttended(String hearingType, String appellantName, boolean presentingOfifficerAttened, String bundlePage) {
        if (StringUtils.equalsIgnoreCase("faceToFace", hearingType)) {
            return appellantName + " attended the hearing today and the Tribunal considered the appeal bundle to page " + bundlePage + ". "
                + (presentingOfifficerAttened ? "A" : "No") + " Presenting Officer attended on behalf of the Respondent.";
        } else {
            return appellantName + " attended and the Tribunal considered the appeal bundle to page " + bundlePage + ". "
                + (presentingOfifficerAttened ? "A" : "No") + " Presenting Officer attended on behalf of the Respondent.";
        }

    }

}
