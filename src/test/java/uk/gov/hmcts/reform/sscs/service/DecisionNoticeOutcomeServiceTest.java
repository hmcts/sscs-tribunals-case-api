package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class DecisionNoticeOutcomeServiceTest {

    private DecisionNoticeOutcomeService service;

    @Before
    public void setup() {
        service = new DecisionNoticeOutcomeService();
    }

    @Test
    @Parameters({
        "higher, higher, decisionInFavourOfAppellant",
        "higher, same, decisionInFavourOfAppellant",
        "higher, lower, decisionUpheld",
        "same, higher, decisionInFavourOfAppellant",
        "same, same, decisionUpheld",
        "same, lower, decisionUpheld",
        "lower, higher, decisionUpheld",
        "lower, same, decisionUpheld",
        "lower, lower, decisionUpheld"})
    public void givenFinalDecisionComparedToDwpQuestionAndAtLeastOneDecisionIsHigherAndNeitherIsLower_thenSetDecisionInFavourOfAppellant(String comparedRateDailyLiving, String comparedRateMobility,
        String expectedOutcome) {

        SscsCaseData caseData = SscsCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving)
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility)
            .writeFinalDecisionIsDescriptorFlow("yes").build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNotNull(outcome);

        assertEquals(expectedOutcome, outcome.getId());
    }

    @Test
    @Parameters({
        "higher, higher, decisionInFavourOfAppellant",
        "higher, same, decisionInFavourOfAppellant",
        "higher, lower, decisionUpheld",
        "same, higher, decisionInFavourOfAppellant",
        "same, same, decisionUpheld",
        "same, lower, decisionUpheld",
        "lower, higher, decisionUpheld",
        "lower, same, decisionUpheld",
        "lower, lower, decisionUpheld"})
    public void givenExplicitAlternatePathRefusalAndFinalDecisionComparedToDwpQuestionAndAtLeastOneDecisionIsHigherAndNeitherIsLower_thenSetDecisionInFavourOfAppellant(String comparedRateDailyLiving, String comparedRateMobility,
        String expectedOutcome) {

        SscsCaseData caseData = SscsCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving)
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility)
            .writeFinalDecisionAllowedOrRefused("refused")
            .writeFinalDecisionIsDescriptorFlow("yes").build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNotNull(outcome);

        assertEquals(expectedOutcome, outcome.getId());
    }


    @Test
    public void givenFinalDecisionComparedToDwpQuestionWithIncorrectValue_ThenReturnNull() {

        SscsCaseData caseData = SscsCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher")
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion("something")
            .writeFinalDecisionIsDescriptorFlow("yes").build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNull(outcome);
    }

    @Test
    public void givenExplicitAltenratePathRefusalFinalDecisionComparedToDwpQuestionWithIncorrectValue_ThenReturnNull() {

        SscsCaseData caseData = SscsCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher")
            .writeFinalDecisionAllowedOrRefused("refused")
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion("something")
            .writeFinalDecisionIsDescriptorFlow("yes").build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNull(outcome);
    }

    @Test
    public void givenFinalDecisionComparedToDwpQuestionWithNullValue_ThenReturnNull() {

        SscsCaseData caseData = SscsCaseData.builder().writeFinalDecisionIsDescriptorFlow("yes")
            .pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher")
            .build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNull(outcome);

    }


    @Test
    @Parameters({
        "higher, higher",
        "higher, same",
        "higher, lower",
        "same, higher",
        "same, same",
        "same, lower",
        "lower, higher",
        "lower, same",
        "lower, lower"})
    public void givenTypeOfAppealNotSetAndRefusedAndOtherParametersSetWithExplicitRefusal_thenSetDecisionInFavourOfAppellant(String comparedRateDailyLiving, String comparedRateMobility) {

        SscsCaseData caseData = SscsCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving)
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility)
            .writeFinalDecisionAllowedOrRefused("refused")
            .build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNull(outcome);
    }

    @Test
    public void givenExplicitAlternatePathRefusalFinalDecisionComparedToDwpQuestionWithNullValue_ThenReturnNull() {

        SscsCaseData caseData = SscsCaseData.builder().writeFinalDecisionIsDescriptorFlow("yes")
            .writeFinalDecisionAllowedOrRefused("refused")
            .pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher")
            .build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNull(outcome);

    }

    @Test
    @Parameters({
        "higher, higher, decisionUpheld",
        "higher, same, decisionUpheld",
        "higher, lower, decisionUpheld",
        "same, higher, decisionUpheld",
        "same, same, decisionUpheld",
        "same, lower, decisionUpheld",
        "lower, higher, decisionUpheld",
        "lower, same, decisionUpheld",
        "lower, lower, decisionUpheld"})
    public void givenNonDailyLivingAndOrMobilityScenarioAndIsRefusedAndIrrelevantParametersSet_thenSetDecisionUpheld(String comparedRateDailyLiving, String comparedRateMobility,
        String expectedOutcome) {

        SscsCaseData caseData = SscsCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving)
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility)
            .writeFinalDecisionIsDescriptorFlow("no")
            .writeFinalDecisionAllowedOrRefused("refused").build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNotNull(outcome);

        assertEquals(expectedOutcome, outcome.getId());
    }

    @Test
    @Parameters({
        "higher, higher, decisionInFavourOfAppellant",
        "higher, same, decisionInFavourOfAppellant",
        "higher, lower, decisionInFavourOfAppellant",
        "same, higher, decisionInFavourOfAppellant",
        "same, same, decisionInFavourOfAppellant",
        "same, lower, decisionInFavourOfAppellant",
        "lower, higher, decisionInFavourOfAppellant",
        "lower, same, decisionInFavourOfAppellant",
        "lower, lower, decisionInFavourOfAppellant"})
    public void givenNonDailyLivingAndOrMobilityScenarioAndIsAllowedAndIrrelevantParametersSet_thenSetDecisionInFavourOfAppellant(String comparedRateDailyLiving, String comparedRateMobility,
        String expectedOutcome) {

        SscsCaseData caseData = SscsCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving)
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility)
            .writeFinalDecisionIsDescriptorFlow("no")
            .writeFinalDecisionAllowedOrRefused("allowed").build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNotNull(outcome);

        assertEquals(expectedOutcome, outcome.getId());
    }

    @Test
    @Parameters({
        "higher, higher",
        "higher, same",
        "higher, lower",
        "same, higher",
        "same, same",
        "same, lower",
        "lower, higher",
        "lower, same",
        "lower, lower"})
    public void givenNonDailyLivingAndOrMobilityScenarioAndExplictOutcomeNotSetAndIrrelevantParametersSet_thenReturnNull(String comparedRateDailyLiving, String comparedRateMobility) {

        SscsCaseData caseData = SscsCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving)
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility)
            .writeFinalDecisionIsDescriptorFlow("no")
            .build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNull(outcome);
    }
}
