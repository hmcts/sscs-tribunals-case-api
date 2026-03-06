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
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.WriteFinalDecisionComponentId;

@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class WriteFinalDecisionTemplateContent {

    private static final String FACETOFACE = "faceToFace";
    private static final String TRIAGE = "triage";
    protected static DateTimeFormatter DATEFORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Getter
    @JsonProperty("template_content")
    private final List<TemplateComponent<?>> components;

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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (TemplateComponent<?> component : components) {
            if (!component.toString().isBlank()) {
                sb.append(component);
                sb.append("\n\n");
            }

        }
        return sb.toString();
    }

    public String getAllowedOrRefusedSentence(boolean allowed) {
        return "The appeal is " + (allowed ? "allowed" : "refused") + ".";
    }

    public String getConfirmedOrSetAsideSentence(boolean setAside, String decisionDate, boolean isHmrc, boolean isIbca) {
        String madeBy;
        if (isHmrc) {
            madeBy = "HM Revenue and Customs";
        } else if (isIbca) {
            madeBy = "the Infected Blood Compensation Authority";
        } else {
            madeBy = "the Secretary of State";
        }

        return String.format("The decision made by %s on %s is %s.", madeBy, DATEFORMATTER.format(LocalDate.parse(decisionDate)), !setAside ? "confirmed" : "set aside");
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

    public List<String> getHearingTypeSentences(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {

        if (equalsIgnoreCase("paper", writeFinalDecisionTemplateBody.getHearingType())) {
            return asList("No party has objected to the matter being decided without a hearing.",
                    "Having considered the appeal bundle to page "
                            + writeFinalDecisionTemplateBody.getPageNumber()
                            + " and the requirements of rules 2 and 27 of the Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that it is able to decide the case in this way.");
        } else  {
            return getFaceToFaceTelephoneVideoHearingTypeSentences(
                    writeFinalDecisionTemplateBody.getHearingType(),
                    writeFinalDecisionTemplateBody.getAppellantName(),
                    writeFinalDecisionTemplateBody.getAppointeeName(),
                    writeFinalDecisionTemplateBody.getPageNumber(),
                    writeFinalDecisionTemplateBody.isAttendedHearing(),
                    writeFinalDecisionTemplateBody.isAppointeeAttended(),
                    writeFinalDecisionTemplateBody.isAppointeeOnCase(),
                    writeFinalDecisionTemplateBody.isPresentingOfficerAttended(),
                    writeFinalDecisionTemplateBody.getOtherPartyNamesAttendedHearing(),
                    writeFinalDecisionTemplateBody.getOtherPartyNamesDidNotAttendHearing());
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

    public void addDescriptorTableIfPopulated(DescriptorTable descriptorTable) {
        if (descriptorTable.getContent() != null && !descriptorTable.getContent().isEmpty()) {
            this.components.add(descriptorTable);
        }
    }

    private String getAppellantAndOtherPartySentences(boolean appellantAttended,
                                                      boolean appointeeAttended,
                                                      String appellantName,
                                                      String appointeeName,
                                                      boolean presentingOfficerAttended,
                                                      String bundlePage,
                                                      List<String> otherPartyNamesAttended,
                                                      List<String> otherPartyNamesDidNotAttend) {

        List<String> sentences = new ArrayList<>();

        var attendedText = getRespondentsAttended(appellantAttended, appellantName, appointeeAttended,
            appointeeName, presentingOfficerAttended, otherPartyNamesAttended);

        var notAttendedText = getRespondentsNotAttend(appellantAttended, appellantName, presentingOfficerAttended,
            otherPartyNamesDidNotAttend);

        if (StringUtils.isNotEmpty(attendedText)) {
            sentences.add("The following people attended: %s".formatted(attendedText));
        }

        if (StringUtils.isNotEmpty(notAttendedText)) {
            sentences.add("%s did not attend".formatted(notAttendedText));
        }

        if (StringUtils.isNotEmpty(bundlePage)) {
            sentences.add(String.format("The Tribunal considered the appeal bundle to page %s", bundlePage));
        }

        return sentences.isEmpty() ? "" : String.join(". ", sentences) + ".";
    }

    private String getRespondentsAttended(boolean appellantAttended,
                                          String appellantName,
                                          boolean appointeeAttended,
                                          String appointeeName,
                                          boolean presentingOfficerAttended,
                                          List<String> otherPartyNamesAttended) {

        List<String> respondents = new ArrayList<>();

        if (appellantAttended) {
            respondents.add(String.format("%s the appellant", appellantName));
        } else if (appointeeAttended) {
            respondents.add(String.format("%s the appointee", appointeeName));
        }

        if (otherPartyNamesAttended != null && !otherPartyNamesAttended.isEmpty()) {
            respondents.addAll(otherPartyNamesAttended);
        }

        if (presentingOfficerAttended) {
            respondents.add(presentingOfficerText(!respondents.isEmpty()));
        }

        return joinRespondentsNameAsStr(respondents);
    }

    private String getRespondentsNotAttend(boolean appellantAttended,
                                           String appellantName,
                                           boolean presentingOfficerAttended,
                                           List<String> otherPartyNamesDidNotAttend) {

        List<String> respondents = new ArrayList<>();

        if (!appellantAttended) {
            respondents.add(String.format("%s the appellant", appellantName));
        }

        if (otherPartyNamesDidNotAttend != null && !otherPartyNamesDidNotAttend.isEmpty()) {
            respondents.addAll(otherPartyNamesDidNotAttend);
        }

        if (!presentingOfficerAttended) {
            respondents.add(presentingOfficerText(!respondents.isEmpty()));
        }

        return joinRespondentsNameAsStr(respondents);
    }

    private String joinRespondentsNameAsStr(List<String> respondents) {
        if (respondents.isEmpty()) {
            return "";
        }

        if (respondents.size() == 1) {
            return respondents.getFirst();
        }

        var lastRespondent = respondents.getLast();

        return String.join(", ", respondents.subList(0, respondents.size() - 1)) + " and " + lastRespondent;
    }

    private String presentingOfficerText(boolean anyOneElseBesideOfficer) {
        return anyOneElseBesideOfficer ? "a representative from the First Tier Agency"
            : "A representative from the First Tier Agency";
    }

    protected String getConsideredParagraph(String bundlePage, String appointeName) {
        return "Having considered the appeal bundle to page " + bundlePage + " and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify " + appointeName + " of the hearing and that it is in the interests of justice to proceed today. ";
    }

    protected String getTriageConsideredParagraph(String bundlePage) {
        return "The tribunal considered the appeal bundle to page " + bundlePage + ".";
    }

    private List<String> getFaceToFaceTelephoneVideoHearingTypeSentences(String hearingType,
                                                                         String appellantName,
                                                                         String appointeeName,
                                                                         String bundlePage,
                                                                         boolean appellantAttended,
                                                                         boolean appointeeAttended,
                                                                         boolean isAppointeeOnCase,
                                                                         boolean presentingOfficerAttended,
                                                                         List<String> otherPartyNamesAttended,
                                                                         List<String> otherPartyNamesDidNotAttend) {

        if (isAppointeeOnCase && appointeeAttended) {
            return getSentenceAppointeeOnCaseAppointeeAttended(hearingType, appellantName, appointeeName,
                presentingOfficerAttended, bundlePage, otherPartyNamesAttended, otherPartyNamesDidNotAttend);
        }

        if (isAppointeeOnCase) {
            return getSentenceAppointeeOnCaseAppointeeNotAttended(hearingType, appellantName, appointeeName, presentingOfficerAttended,
                bundlePage, otherPartyNamesAttended, otherPartyNamesDidNotAttend);
        }

        if (appellantAttended) {
            return getSentenceAppointeeNotOnCaseAppellantAttended(hearingType,
                appellantName, appointeeName, presentingOfficerAttended, bundlePage,
                otherPartyNamesAttended, otherPartyNamesDidNotAttend);
        }

        return getSentenceAppointeeNotOnCaseAppellantNotAttended(hearingType, appellantName,
            appointeeName, presentingOfficerAttended, bundlePage, otherPartyNamesAttended, otherPartyNamesDidNotAttend);
    }

    private List<String> getSentenceAppointeeOnCaseAppointeeAttended(String hearingType,
                                                                     String appellantName,
                                                                     String appointeeName,
                                                                     boolean presentingOfficerAttended,
                                                                     String bundlePage,
                                                                     List<String> otherPartyNamesAttended,
                                                                     List<String> otherPartyNamesDidNotAttend) {
        if (equalsIgnoreCase(FACETOFACE, hearingType)) {
            return singletonList(
                getFaceToFaceTypeSentences()
                    + getAppellantAndOtherPartySentences(
                    false,
                    true,
                    appellantName,
                    appointeeName,
                    presentingOfficerAttended,
                    bundlePage, otherPartyNamesAttended, otherPartyNamesDidNotAttend));
        } else if (equalsIgnoreCase(TRIAGE, hearingType)) {
            return singletonList(getTriageConsideredParagraph(bundlePage));
        } else {
            return singletonList(
                getNonFaceToFaceTypeSentences(hearingType)
                    + getAppellantAndOtherPartySentences(
                    false,
                    true,
                    appellantName,
                    appointeeName,
                    presentingOfficerAttended,
                    bundlePage,
                    otherPartyNamesAttended, otherPartyNamesDidNotAttend));
        }
    }

    private List<String> getSentenceAppointeeOnCaseAppointeeNotAttended(String hearingType,
                                                                        String appellantName,
                                                                        String appointeeName,
                                                                        boolean presentingOfficerAttended,
                                                                        String bundlePage,
                                                                        List<String> otherPartyNamesAttended,
                                                                        List<String> otherPartyNamesDidNotAttend) {
        if (equalsIgnoreCase(FACETOFACE, hearingType)) {
            return asList(
                getFaceToFaceTypeSentences()
                    + getAppellantAndOtherPartySentences(
                    true,
                    false,
                    appellantName,
                    appointeeName,
                    presentingOfficerAttended,
                    bundlePage, otherPartyNamesAttended, otherPartyNamesDidNotAttend),
                getConsideredParagraph(bundlePage, appointeeName));
        } else if (equalsIgnoreCase(TRIAGE, hearingType)) {
            return singletonList(getTriageConsideredParagraph(bundlePage));
        } else {
            return asList(
                getNonFaceToFaceTypeSentences(hearingType)
                    + getAppellantAndOtherPartySentences(
                    true,
                    false,
                    appellantName,
                    appointeeName,
                    presentingOfficerAttended,
                    bundlePage, otherPartyNamesAttended, otherPartyNamesDidNotAttend),
                getConsideredParagraph(bundlePage, appointeeName));
        }
    }

    private List<String> getSentenceAppointeeNotOnCaseAppellantAttended(String hearingType,
                                                                        String appellantName,
                                                                        String appointeeName,
                                                                        boolean presentingOfficerAttended,
                                                                        String bundlePage,
                                                                        List<String> otherPartyNamesAttended,
                                                                        List<String> otherPartyNamesDidNotAttendHearing) {
        if (equalsIgnoreCase(FACETOFACE, hearingType)) {
            return singletonList(
                getFaceToFaceTypeSentences()
                    + getAppellantAndOtherPartySentences(
                    true,
                    false,
                    appellantName,
                    appointeeName,
                    presentingOfficerAttended,
                    bundlePage,
                    otherPartyNamesAttended, otherPartyNamesDidNotAttendHearing));
        } else if (equalsIgnoreCase(TRIAGE, hearingType)) {
            return singletonList(getTriageConsideredParagraph(bundlePage));
        } else {
            return singletonList(
                getNonFaceToFaceTypeSentences(hearingType)
                    + getAppellantAndOtherPartySentences(
                    true,
                    false,
                    appellantName,
                    appointeeName,
                    presentingOfficerAttended,
                    bundlePage,
                    otherPartyNamesAttended, otherPartyNamesDidNotAttendHearing));
        }
    }

    private List<String> getSentenceAppointeeNotOnCaseAppellantNotAttended(String hearingType,
                                                                           String appellantName, String appointeeName,
                                                                           boolean presentingOfficerAttended,
                                                                           String bundlePage,
                                                                           List<String> otherPartyNamesAttended,
                                                                           List<String> otherPartyNamesDidNotAttend) {
        if (equalsIgnoreCase(FACETOFACE, hearingType)) {
            return asList(
                getFaceToFaceTypeSentences()
                    + getAppellantAndOtherPartySentences(false, false, appellantName, appointeeName,
                    presentingOfficerAttended, null, otherPartyNamesAttended,
                    otherPartyNamesDidNotAttend),
                getConsideredParagraph(bundlePage, appellantName));
        } else if (equalsIgnoreCase(TRIAGE, hearingType)) {
            return singletonList(getTriageConsideredParagraph(bundlePage));
        } else {
            return asList(
                getNonFaceToFaceTypeSentences(hearingType)
                    + getAppellantAndOtherPartySentences(false, false, appellantName, appointeeName,
                    presentingOfficerAttended, null, otherPartyNamesAttended,
                    otherPartyNamesDidNotAttend),
                getConsideredParagraph(bundlePage, appellantName));
        }
    }

    private String getFaceToFaceTypeSentences() {
        return "This has been an oral (face to face) hearing. ";
    }

    private String getNonFaceToFaceTypeSentences(String hearingType)  {
        return "This has been a remote hearing in the form of a " + hearingType + " hearing. ";
    }

    public void addHearingType(WriteFinalDecisionTemplateBody writeFinalDecisionTemplateBody) {
        for (String hearingTypeSentence : getHearingTypeSentences(writeFinalDecisionTemplateBody)) {
            addComponent(new Paragraph(WriteFinalDecisionComponentId.HEARING_TYPE.name(), hearingTypeSentence));
        }
    }

    public void addComponent(TemplateComponent<?> component) {
        this.components.add(component);
    }
}
