package uk.gov.hmcts.reform.sscs.model.docassembly;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.apache.commons.lang3.StringUtils.startsWith;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionComponentId;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class WriteFinalDecisionTemplateContent {

    protected static DateTimeFormatter DATEFORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @JsonProperty("template_content")
    private List<TemplateComponent<?>> components;

    public WriteFinalDecisionTemplateContent() {
        this.components = new ArrayList<>();
    }

    public List<TemplateComponent<?>> getComponents() {
        return components;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (TemplateComponent<?> component : components) {
            if (!component.toString().isBlank()) {
                sb.append(component.toString());
                sb.append("\n\n");
            }

        }
        return sb.toString();
    }

    public String getAllowedOrRefusedSentence(boolean allowed) {
        return "The appeal is " + (allowed ? "allowed" : "refused") + ".";
    }

    public String getConfirmedOrSetAsideSentence(boolean setAside, String decisionDate) {
        return "The decision made by the Secretary of State on " + DATEFORMATTER.format(LocalDate.parse(decisionDate)) + " is "
            + (!setAside ? "confirmed." : "set aside.");
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

    public void addReasonsIfPresent(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        if (writeFinalDecisionTemplateBody.getReasonsForDecision() != null) {
            for (String reason : writeFinalDecisionTemplateBody.getReasonsForDecision()) {
                addComponent(new Paragraph(WriteFinalDecisionComponentId.REASON.name(), reason));
            }
        }
    }

    public List<String> getHearingTypeSentences(String appellantName, String bundlePage, String hearingType, boolean appellantAttended, boolean presentingOfficerAttened) {
        if (equalsIgnoreCase("paper", hearingType)) {
            return asList("No party has objected to the matter being decided without a hearing.", "Having considered the appeal bundle to page " + bundlePage + " and the requirements of rules 2 and 27 of the Tribunal Procedure (First-tier Tribunal) (Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that it is able to decide the case in this way.");
        } else  {
            return getFaceToFaceTelephoneVideoHearingTypeSentences(hearingType, appellantName, bundlePage, appellantAttended, presentingOfficerAttened);
        }
    }

    public void addAnythingElseIfPresent(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        if (StringUtils.isNotBlank(writeFinalDecisionTemplateBody.getAnythingElse())) {
            addComponent(new Paragraph(WriteFinalDecisionComponentId.ANYTHING_ELSE.name(), writeFinalDecisionTemplateBody.getAnythingElse()));
        }
    }

    public void addRecommendationIfPresent(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        if (writeFinalDecisionTemplateBody.getDwpReassessTheAward() != null) {
            addComponent(new Paragraph(WriteFinalDecisionComponentId.RECOMMENDATION.name(),
                getRecommendationSentence(writeFinalDecisionTemplateBody.getDwpReassessTheAward(), writeFinalDecisionTemplateBody.getAppellantName())));
        }
    }

    public void addDescriptorTableIfPopulated(DescriptorTable desciptorTable) {
        if (desciptorTable.getContent() != null && !desciptorTable.getContent().isEmpty()) {
            this.components.add(desciptorTable);
        }
    }

    protected String getAppellantAttended(String hearingType, String appellantName, boolean presentingOfifficerAttened, String bundlePage) {
        if (equalsIgnoreCase("faceToFace", hearingType)) {
            return appellantName + " attended the hearing today and the Tribunal considered the appeal bundle to page " + bundlePage + ". "
                + (presentingOfifficerAttened ? "A" : "No") + " Presenting Officer attended on behalf of the Respondent.";
        } else {
            return appellantName + " attended and the Tribunal considered the appeal bundle to page " + bundlePage + ". "
                + (presentingOfifficerAttened ? "A" : "No") + " Presenting Officer attended on behalf of the Respondent.";
        }
    }

    protected String getConsideredParagraph(String bundlePage, String appellantName) {
        return "Having considered the appeal bundle to page " + bundlePage + " and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify " + appellantName + " of the hearing and that it is in the interests of justice to proceed today. ";
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
            addComponent(new Paragraph(WriteFinalDecisionComponentId.HEARING_TYPE.name(), hearingTypeSentence));
        }
    }

    public void addComponent(TemplateComponent<?> component) {
        this.components.add(component);
    }
}
