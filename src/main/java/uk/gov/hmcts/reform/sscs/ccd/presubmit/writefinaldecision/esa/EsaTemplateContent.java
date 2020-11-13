package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.*;
import static org.apache.commons.lang3.StringUtils.startsWith;

import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios.EsaScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateContent;

public abstract class EsaTemplateContent extends WriteFinalDecisionTemplateContent {

    public String getAllowedOrRefusedSentence(boolean allowed) {
        return "The appeal is " + (allowed ? "allowed" : "refused") + ".";
    }

    public String getConfirmedOrSetAsideSentence(boolean setAside, String decisionDate) {
        return "The decision made by the Secretary of State on " + decisionDate + " is "
            + (!setAside ? "confirmed." : "set aside.");
    }

    public String getDoesNotHaveLimitedCapabilityForWorkSentence(String appellantName) {
        return appellantName + " does not have limited capability for work and cannot be treated as having limited capability for work.";
    }

    public String getDoesNotHaveLimitedCapabilityForWorkNoSchedule3Sentence(String appellantName) {
        return appellantName + " does not have limited capability for work-related activity because no descriptor from Schedule 3 applied.  Regulation 35 did not apply.";
    }

    public String getDoesHaveLimitedCapabilityForWorkSentence(String appellantName, boolean isTreatedLimitedCapability, boolean includeWorkRelatedActivities, boolean isWorkRelatedActivitiesLimited) {
        return appellantName + (isTreatedLimitedCapability ? " is to be treated as having" : " has") + " limited capability for work" + (includeWorkRelatedActivities ? " and " + (isWorkRelatedActivitiesLimited ? "has limited capability " : "") + "for work-related activity." : ".");
    }

    public String getDoesHaveLimitedCapabilityForWorkRelatedSentence(String appellantName) {
        return appellantName + " is to be treated as having limited capability for work-related activity.";
    }

    public String getSecretaryOfStateAcceptsHasLimitedCapabilityForWorkSentence(String appellantName) {
        return "The Secretary of State has accepted that " + appellantName + " has limited capability for "
                + "work related activity. This was not an issue.";
    }

    public String getHasLimitedCapabilityForWorkNoSchedule3SentenceReg35Applies() {
        return "No descriptor from Schedule 3 of the Employment and Support Allowance (ESA) Regulations 2008 was satisfied but regulation 35 applied.";
    }


    public String getSchedule2PointsSentence(Integer points, Boolean isSufficient) {
        return "In applying the work capability assessment " + points + (points == 1 ? " point was" : " points were")
            + " scored from the activities and descriptors in Schedule "
            + "2 of the ESA Regulations 2008" + (isSufficient != null && isSufficient.booleanValue() ? " made up as follows:"
            : ". This is insufficient to meet the "
            + "threshold for the test. Regulation 29 of the Employment and Support Allowance (ESA) Regulations 2008 did not apply.");
    }

    public String getInsufficientPointsSentenceRegulation29Applied() {
        return "This is because insufficient points were scored to meet the threshold for the work capability assessment, "
                + "but regulation 29 of the Employment and Support Allowance (ESA) Regulations 2008 applied.";
    }

    public String getInsufficientPointsSentenceRegulation29AndRegulation35Applied() {
        return "This is because insufficient points were scored to meet the threshold for the work capability assessment "
                + "and none of the Schedule 3 activities and descriptors were satisfied, but the tribunal applied regulations 29 and 35 of the Employment and Support Allowance Regulations (ESA) 2008.";
    }

    public String getInsufficientPointsSentenceRegulation29AndSchedule3Applied() {
        return "This is because insufficient points were scored under Schedule 2 of the Employment and Support Allowance (ESA) Regulations 2008 to meet the threshold for the Work Capability Assessment, but the tribunal applied regulation 29.";
    }

    public String getRegulation29And35DiseaseOrDisablementSentence(String appellantName, boolean isRegulation35Applied) {
        //FIXME: Replace disease or disablement as part of future ticket
        return "The tribunal applied regulation" + (isRegulation35Applied ? "s" : "") + " 29 " + (isRegulation35Applied ? "and 35 " : "")
            + "because it found that " + appellantName + " suffers from [insert disease or disablement] and, by reasons of such disease or disablement, "
            + "there would be a substantial risk to the mental or physical health of any person if they were found not to have limited capability for work"
            + (isRegulation35Applied ? " and for work-related activity." : ".");
    }

    public String getRegulation35DiseaseOrDisablementSentence(String appellantName) {
        //FIXME: Replace disease or disablement as part of future ticket
        return "The tribunal applied that regulation because it found that " + appellantName + " suffers from "
                + "[insert disease or disablement] and, by reasons of such disease or disablement, there would "
                + "be a substantial risk to the mental or physical health of any person if they were found not to have limited capability for work.";
    }

    public String getRegulation35DiseaseOrDisablementSentenceWorkRelated(String appellantName) {
        //FIXME: Replace disease or disablement as part of future ticket
        return "The tribunal applied that regulation because it found that " + appellantName + " suffers from "
                + "[insert disease or disablement] and, by reasons of such disease or disablement, there would "
                + "be a substantial risk to the mental or physical health of any person if they were found not to have limited capability for work-related activity.";
    }

    public String getSchedule3AppliesParagraph() {
        return "The following activity and descriptor from Schedule 3 applied:";
    }

    public String getHearingTypeSentence(String appellantName, String bundlePage) {
        // Placeholder for SSCS-8033 (Chris D)
        return "This has been an oral (face to face) hearing. "
                + appellantName + " attended the hearing today and the tribunal considered the appeal bundle to page " + bundlePage
                + ". A Presenting Officer attended on behalf of the Respondent.";
    }

    public String getRecommendationSentence(String code, String appellantName) {
        final String firstSentence = "Any recommendation given below does not form part of the Tribunal's decision and is not binding on the Secretary of State.";
        final String secondSentence;
        if ("noRecommendation".equals(code)) {
            secondSentence = format("The Tribunal makes no recommendation as to when the Department should reassess %s ", appellantName);
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


    public abstract EsaScenario getScenario();
}
