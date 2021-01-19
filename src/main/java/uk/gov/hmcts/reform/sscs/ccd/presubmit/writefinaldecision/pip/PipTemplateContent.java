package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import java.time.LocalDate;
import java.util.List;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios.PipScenario;
import uk.gov.hmcts.reform.sscs.model.docassembly.Descriptor;
import uk.gov.hmcts.reform.sscs.model.docassembly.WriteFinalDecisionTemplateContent;

public abstract class PipTemplateContent extends WriteFinalDecisionTemplateContent {

    @Override
    protected String getBenefitTypeNameWithoutInitials() {
        return "Personal Independence Payment";
    }

    protected String getRegulationsYear() {
        return null;
    }

    @Override
    protected String getBenefitTypeInitials() {
        return "PIP";
    }

    protected String getIsEntitledDailyLiving(String appellantName, String dailyLivingRate, String startDate, String endDate) {
        return appellantName + " is entitled to the daily living component at the " + dailyLivingRate + " from " + DATEFORMATTER.format(LocalDate.parse(startDate))  + (endDate == null ? " for an indefinite period." : (" to " + DATEFORMATTER.format(LocalDate.parse(endDate)) + "."));
    }

    @Override
    protected String getAppellantAttended(String hearingType, String appellantName, boolean presentingOfifficerAttened, String bundlePage) {
        return appellantName + " attended the hearing today and the tribunal considered the appeal bundle to page " + bundlePage + ". "
                + (presentingOfifficerAttened ? "A" : "No") + " Presenting Officer attended on behalf of the Respondent.";
    }

    @Override
    protected String getConsideredParagraph(String bundlePage, String appellantName) {
        return "Having considered the appeal bundle to page " + bundlePage + " and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal) (Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify " + appellantName + " of the hearing and that it is in the interests of justice to proceed today. ";
    }

    /*

     protected String getConsideredParagraph(String bundlePage, String appellantName) {
        return "Having considered the appeal bundle to page " + bundlePage + " and the requirements of rules 2 and 31 of The Tribunal Procedure (First-tier Tribunal)(Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that reasonable steps were taken to notify " + appellantName + " of the hearing and that it is in the interests of justice to proceed today. ";
    }
     */

    protected String getIsEntitledMobility(String appellantName, String mobilityRate, String startDate, String endDate) {
        return appellantName + " is entitled to the mobility component at the " + mobilityRate + " from " + DATEFORMATTER.format(LocalDate.parse(startDate))  + (endDate == null ? " for an indefinite period." : (" to " + DATEFORMATTER.format(LocalDate.parse(endDate)) + "."));
    }

    @Override
    public String getConfirmedOrSetAsideSentence(boolean setAside, String decisionDate) {
        return "The decision made by the Secretary of State on " + DATEFORMATTER.format(LocalDate.parse(decisionDate)) + " in respect of Personal Independence Payment is "
                + (!setAside ? "confirmed." : "set aside.");
    }

    /*
    public String getConfirmedOrSetAsideSentence(boolean setAside, String decisionDate) {
        return "The decision made by the Secretary of State on " + DATEFORMATTER.format(LocalDate.parse(decisionDate)) + " is "
            + (!setAside ? "confirmed." : "set aside.");
     */

    @Override
    public List<String> getHearingTypeSentences(String appellantName, String bundlePage, String hearingType, boolean appellantAttended, boolean presentingOfficerAttened) {
        if (equalsIgnoreCase("paper", hearingType)) {
            // Single output.
            return asList("No party has objected to the matter being decided without a hearing. Having considered the appeal bundle to page " + bundlePage + " and the requirements of rules 2 and 27 of the Tribunal Procedure (First-tier Tribunal) (Social Entitlement Chamber) Rules 2008 the Tribunal is satisfied that it is able to decide the case in this way.");
        } else  {
            return getFaceToFaceTelephoneVideoHearingTypeSentences(hearingType, appellantName, bundlePage, appellantAttended, presentingOfficerAttened);
        }
    }

    @Override // Remove the hearing
    public List<String> getFaceToFaceTelephoneVideoHearingTypeSentences(String hearingType, String appellantName, String bundlePage,
        boolean appellantAttended, boolean presentingOfifficerAttened) {
        if (appellantAttended) {
            if (equalsIgnoreCase("faceToFace", hearingType)) {
                return singletonList("This has been an oral (face to face) hearing. "
                    + getAppellantAttended(hearingType, appellantName, presentingOfifficerAttened, bundlePage));
            } else if (equalsIgnoreCase("triage", hearingType)) {
                return singletonList(getTriageConsideredParagraph(bundlePage));
            } else if (equalsIgnoreCase("paper", hearingType)) {
                return singletonList("This has been a remote hearing in the form of a " + hearingType + " hearing. " + getAppellantAttended(hearingType, appellantName, presentingOfifficerAttened, bundlePage));
            } else {
                return singletonList("This has been a remote hearing in the form of a " + hearingType + " hearing. "
                    + getAppellantAttended(hearingType, appellantName, presentingOfifficerAttened, bundlePage));
            }
        } else {
            if (equalsIgnoreCase("faceToFace", hearingType)) {
                // Adding in // This has been
                return asList("This has been an oral (face to face) hearing. " + appellantName + " requested an oral hearing but did not attend today. "
                        + (presentingOfifficerAttened ? "A " : "No ") + "Presenting Officer attended on behalf of the Respondent.",
                    getConsideredParagraph(bundlePage, appellantName));
            } else if (equalsIgnoreCase("triage", hearingType)) {
                // Removed "the hearing"
                return asList(appellantName + " did not attend today. "
                        + (presentingOfifficerAttened ? "A" : "No") + " Presenting Officer attended on behalf of the Respondent.",
                    getConsideredParagraph(bundlePage, appellantName));
            } else if (equalsIgnoreCase("paper", hearingType)) {
                // Removed "the hearing"
                return asList("This has been a remote hearing in the form of a " + hearingType + " hearing. " + appellantName + " did not attend today. "
                        + (presentingOfifficerAttened ? "A" : "No") + " Presenting Officer attended on behalf of the Respondent. " + getConsideredParagraph(bundlePage, appellantName));
            } else {
                // Removed "the hearing"
                return asList("This has been a remote hearing in the form of a " + hearingType + " hearing. " + appellantName + " did not attend today. "
                        + (presentingOfifficerAttened ? "A" : "No") + " Presenting Officer attended on behalf of the Respondent.",
                    getConsideredParagraph(bundlePage, appellantName));
            }
        }
    }

    public String getDailyLivingNotConsidered() {
        return "Only the mobility component was in issue on this appeal and the daily living component was not considered.";
    }

    public String getMobilityNotConsidered() {
        return "Only the daily living component was in issue on this appeal and the mobility component was not considered. ";
    }
    
    public String getMobilityNoAward(String appellantName, String startDate, int points) {
        return appellantName + " does not qualify for an award of the mobility component from " + DATEFORMATTER.format(LocalDate.parse(startDate)) + ". They score " + points + " point" + (points == 1 ? "" : "s") + ". This is insufficient to meet the threshold for the test.";
    }

    public String getDailyLivingNoAward(String appellantName, String startDate, int points) {
        return appellantName + " is not entitled to the daily living component from " + DATEFORMATTER.format(LocalDate.parse(startDate)) + ". They score " + points + " point" + (points == 1 ? "" : "s") + ". This is insufficient to meet the threshold for the test.";
    }
    
    protected String getLimitedAbilityDailyLiving(String appellantName, Integer points, List<Descriptor> descriptors, boolean dailyLivingIsSeverelyLimited) {
        if (points == null) {
            points = 0;
        }
        return appellantName + " has " + (dailyLivingIsSeverelyLimited ? "severely " : "") +  "limited ability to carry out the activities of daily living set out below. They score " + points + " point" + (points == 1 ? "." : "s.") + (descriptors != null && descriptors.size() > 0 ? " They satisfy the following descriptors:" : "");
    }

    protected String getLimitedAbilityMobility(String appellantName, Integer points, List<Descriptor> descriptors, boolean mobililtyIsSeverelyLimited) {
        if (points == null) {
            points = 0;
        }
        return appellantName + " is " + (mobililtyIsSeverelyLimited ? "severely " : "") + "limited in their ability to mobilise. They score " + points + " point" + (points == 1 ? "." : "s.") + (descriptors != null && descriptors.size() > 0 ? "They satisfy the following descriptors:" : "");
    }

    public abstract PipScenario getScenario();
}
