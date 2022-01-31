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
public abstract class WriteFinalDecisionTemplateContent {

    protected static DateTimeFormatter DATEFORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @JsonProperty("template_content")
    private List<TemplateComponent<?>> components;

    private boolean isBenefitTypeAlreadyMentioned;
    private boolean isRegulationsAlreadyMentioned;

    protected abstract String getBenefitTypeNameWithoutInitials();

    protected abstract String getBenefitTypeInitials();

    /**
     * Please note, this method is not idempotent.
     *
     * <p>The first time this method is called, the string returned should be the fully expanded
     * benefit type with the initials in brackets.  eg ("Employment Support Allowance (ESA)")</p>
     *
     * <p>Subsequently, it should return just the initials eg ("ESA")</p>
     * @return The usage dependent benefit type string.
     */
    protected String getUsageDependentBenefitTypeString() {
        String prefix = isBenefitTypeAlreadyMentioned ? "" : (getBenefitTypeNameWithoutInitials() + " ");
        String suffix = isBenefitTypeAlreadyMentioned ? getBenefitTypeInitials() : ("(" + getBenefitTypeInitials() + ")");
        isBenefitTypeAlreadyMentioned = true;
        return prefix + suffix;
    }

    protected abstract String getRegulationsYear();

    /**
     * Please note, this method is not idempotent.
     *
     * <p></p>The first time this method is called it should return the usage-dependent
     * benefit type string (see getUsageDependentBenefitTypeString method) followed by:</p>
     *
     * <p></p>a) The word "Regulations" followed by the year of the regulations
     * on first method call (eg. "ESA Regulations 2008", or "Employment and Support Allowance (ESA) Regulations 2008" ).</p>
     * or
     * b) The word "Regulations" only ( without the year) for subsequent method calls
     * eg. (eg. "ESA Regulations", or "Employment and Support Allowance (ESA) Regulations" )
     * @return The usage dependent benefit type regulations string.
     */
    protected String getUsageDependentBenefitTypeRegulationsString() {
        String value = isRegulationsAlreadyMentioned ? (getUsageDependentBenefitTypeString() + " Regulations") : (getUsageDependentBenefitTypeString() + " Regulations " + getRegulationsYear());
        isRegulationsAlreadyMentioned = true;
        return value;
    }

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

    public List<String> getHearingTypeSentences(String appellantName, String bundlePage, String hearingType, boolean appellantAttended, boolean presentingOfficerAttened, String otherPartyNamesAttended) {
        if (equalsIgnoreCase("paper", hearingType)) {
            return asList("No party has objected to the matter being decided without a hearing.", "Having considered the appeal bundle to page " + bundlePage + " and the requirements of rules 2 and 27 of the Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that it is able to decide the case in this way.");
        } else  {
            return getFaceToFaceTelephoneVideoHearingTypeSentences(hearingType, appellantName, bundlePage, appellantAttended, presentingOfficerAttened, otherPartyNamesAttended);
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

    protected String getAppellantAndOtherPartyAttended(String hearingType, String appellantName, boolean presentingOfifficerAttened, String bundlePage, String otherPartyNamesAttended) {
        if (equalsIgnoreCase("faceToFace", hearingType)) {
            return appellantName + getOtherPartyNamesAttendedString(otherPartyNamesAttended) + " attended the hearing today and the Tribunal considered the appeal bundle to page " + bundlePage + ". "
                + (presentingOfifficerAttened ? "A" : "No") + " Presenting Officer attended on behalf of the Respondent.";
        } else {
            return appellantName + getOtherPartyNamesAttendedString(otherPartyNamesAttended) + " attended and the Tribunal considered the appeal bundle to page " + bundlePage + ". "
                + (presentingOfifficerAttened ? "A" : "No") + " Presenting Officer attended on behalf of the Respondent.";
        }
    }

    protected String getOtherPartyNamesAttendedString(String otherPartyNamesAttended) {
        if (StringUtils.isNotEmpty(otherPartyNamesAttended)) {
            return ", " + otherPartyNamesAttended;
        }
        return "";
    }

    protected String getConsideredParagraph(String bundlePage, String appellantName) {
        return "Having considered the appeal bundle to page " + bundlePage + " and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify " + appellantName + " of the hearing and that it is in the interests of justice to proceed today. ";
    }

    protected String getTriageConsideredParagraph(String bundlePage) {
        return "The tribunal considered the appeal bundle to page " + bundlePage + ".";
    }

    public List<String> getFaceToFaceTelephoneVideoHearingTypeSentences(String hearingType, String appellantName, String bundlePage,
        boolean appellantAttended, boolean presentingOfifficerAttened, String otherPartyNamesAttended) {
        if (appellantAttended) {
            if (equalsIgnoreCase("faceToFace", hearingType)) {
                return singletonList("This has been an oral (face to face) hearing. "
                    + getAppellantAndOtherPartyAttended(hearingType, appellantName, presentingOfifficerAttened, bundlePage, otherPartyNamesAttended));
            } else if (equalsIgnoreCase("triage", hearingType)) {
                return singletonList(getTriageConsideredParagraph(bundlePage));
            } else {
                return singletonList("This has been a remote hearing in the form of a " + hearingType + " hearing. "
                    + getAppellantAndOtherPartyAttended(hearingType, appellantName, presentingOfifficerAttened, bundlePage, otherPartyNamesAttended));
            }
        } else {
            if (equalsIgnoreCase("faceToFace", hearingType)) {
                return asList(appellantName + " requested an oral hearing but did not attend today. "
                        + (presentingOfifficerAttened ? "A " : "No ") + "Presenting Officer attended on behalf of the Respondent.",
                    getConsideredParagraph(bundlePage, appellantName));
            } else if (equalsIgnoreCase("triage", hearingType)) {
                return singletonList(getTriageConsideredParagraph(bundlePage));
            } else {
                return asList("This has been a remote hearing in the form of a " + hearingType + " hearing. " + appellantName + " did not attend the hearing today. "
                        + (presentingOfifficerAttened ? "A" : "No") + " Presenting Officer attended on behalf of the Respondent.",
                    getConsideredParagraph(bundlePage, appellantName));
            }
        }
    }

    public void addHearingType(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        for (String hearingTypeSentence : getHearingTypeSentences(writeFinalDecisionTemplateBody.getAppellantName(), writeFinalDecisionTemplateBody.getPageNumber(),
            writeFinalDecisionTemplateBody.getHearingType(), writeFinalDecisionTemplateBody.isAttendedHearing(), writeFinalDecisionTemplateBody.isPresentingOfficerAttended(),
                writeFinalDecisionTemplateBody.getOtherPartyNamesAttendedHearing())) {
            addComponent(new Paragraph(WriteFinalDecisionComponentId.HEARING_TYPE.name(), hearingTypeSentence));
        }
    }

    public void addComponent(TemplateComponent<?> component) {
        this.components.add(component);
    }
}
