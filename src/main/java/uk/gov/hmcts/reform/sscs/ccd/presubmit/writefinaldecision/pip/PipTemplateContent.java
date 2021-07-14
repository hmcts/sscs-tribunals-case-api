package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

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

    protected String getIsEntitledMobility(String appellantName, String mobilityRate, String startDate, String endDate) {
        return appellantName + " is entitled to the mobility component at the " + mobilityRate + " from " + DATEFORMATTER.format(LocalDate.parse(startDate))  + (endDate == null ? " for an indefinite period." : (" to " + DATEFORMATTER.format(LocalDate.parse(endDate)) + "."));
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
        return appellantName + " is " + (mobililtyIsSeverelyLimited ? "severely " : "") + "limited in their ability to mobilise. They score " + points + " point" + (points == 1 ? "." : "s.") + (descriptors != null && descriptors.size() > 0 ? " They satisfy the following descriptors:" : "");
    }

    public abstract PipScenario getScenario();
}
