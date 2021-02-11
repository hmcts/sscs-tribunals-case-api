package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsPipCaseData;

@RunWith(JUnitParamsRunner.class)
public class DecisionNoticeOutcomeServiceTest {

    private DecisionNoticeOutcomeService service;

    @Before
    public void setup() throws IOException {
        service = new PipDecisionNoticeOutcomeService(new PipDecisionNoticeQuestionService());
    }

    @Test
    @Parameters({
        "higher, higher, decisionInFavourOfAppellant",
        "higher, same, decisionInFavourOfAppellant",
        "higher, lower, decisionInFavourOfAppellant",
        "same, higher, decisionInFavourOfAppellant",
        "same, same, decisionUpheld",
        "same, lower, decisionUpheld",
        "lower, higher, decisionInFavourOfAppellant",
        "lower, same, decisionUpheld",
        "lower, lower, decisionUpheld"})
    public void givenFinalDecisionComparedToDwpQuestionAndAtLeastOneDecisionIsHigher_thenSetDecisionInFavourOfAppellant(String comparedRateDailyLiving, String comparedRateMobility,
        String expectedOutcome) {

        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving)
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility)
                .build())
            .writeFinalDecisionGenerateNotice("yes")
            .writeFinalDecisionIsDescriptorFlow("yes").build();

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
        "same, same, decisionUpheld",
        "same, lower, decisionUpheld",
        "lower, higher, decisionInFavourOfAppellant",
        "lower, same, decisionUpheld",
        "lower, lower, decisionUpheld"})
    public void givenExplicitAlternatePathRefusalAndFinalDecisionComparedToDwpQuestionAndAtLeastOneDecisionIsHigher_thenSetDecisionInFavourOfAppellant(String comparedRateDailyLiving, String comparedRateMobility,
        String expectedOutcome) {

        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving)
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility)
                .build())
            .writeFinalDecisionGenerateNotice("yes")
            .writeFinalDecisionAllowedOrRefused("refused")
            .writeFinalDecisionIsDescriptorFlow("yes").build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNotNull(outcome);

        assertEquals(expectedOutcome, outcome.getId());
    }


    @Test
    public void givenFinalDecisionComparedToDwpQuestionWithIncorrectValue_ThenReturnNull() {

        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher")
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion("something")
                .build())
            .writeFinalDecisionGenerateNotice("yes")
            .writeFinalDecisionIsDescriptorFlow("yes").build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNull(outcome);
    }

    @Test
    public void givenExplicitAlternatePathRefusalFinalDecisionComparedToDwpQuestionWithIncorrectValue_ThenReturnNull() {

        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher")
                .pipWriteFinalDecisionComparedToDwpMobilityQuestion("something")
                .build())
            .writeFinalDecisionAllowedOrRefused("refused")
            .writeFinalDecisionGenerateNotice("yes")
            .writeFinalDecisionIsDescriptorFlow("yes").build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNull(outcome);
    }

    @Test
    public void givenFinalDecisionComparedToDwpQuestionWithNullValue_ThenReturnNull() {

        SscsCaseData caseData = SscsCaseData.builder().writeFinalDecisionIsDescriptorFlow("yes")
            .writeFinalDecisionGenerateNotice("yes")
                .pipSscsCaseData(SscsPipCaseData.builder()
            .pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher")
                        .build())
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

        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving)
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility)
                .build())
            .writeFinalDecisionGenerateNotice("yes")
            .writeFinalDecisionAllowedOrRefused("refused")
            .build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNull(outcome);
    }

    @Test
    public void givenExplicitAlternatePathRefusalFinalDecisionComparedToDwpQuestionWithNullValue_ThenReturnNull() {

        SscsCaseData caseData = SscsCaseData.builder().writeFinalDecisionIsDescriptorFlow("yes")
            .writeFinalDecisionAllowedOrRefused("refused")
            .writeFinalDecisionGenerateNotice("yes")
                .pipSscsCaseData(SscsPipCaseData.builder()
            .pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher")
                        .build())
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

        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving)
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility)
                .build())
            .writeFinalDecisionIsDescriptorFlow("no")
            .writeFinalDecisionGenerateNotice("yes")
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

        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving)
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility)
                .build())
            .writeFinalDecisionIsDescriptorFlow("no")
            .writeFinalDecisionGenerateNotice("yes")
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

        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving)
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility)
                .build())
            .writeFinalDecisionGenerateNotice("yes")
            .writeFinalDecisionIsDescriptorFlow("no")
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
    public void givenManualUploadNonDailyLivingAndOrMobilityScenarioAndIsRefusedAndIrrelevantParametersSet_thenSetDecisionUpheld(String comparedRateDailyLiving, String comparedRateMobility,
        String expectedOutcome) {

        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving)
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility)
                .build())
            .writeFinalDecisionIsDescriptorFlow("no")
            .writeFinalDecisionGenerateNotice("no")
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
    public void givenManualUploadNonDailyLivingAndOrMobilityScenarioAndIsAllowedAndIrrelevantParametersSet_thenSetDecisionInFavourOfAppellant(String comparedRateDailyLiving, String comparedRateMobility,
        String expectedOutcome) {

        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving)
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility)
                .build())
            .writeFinalDecisionIsDescriptorFlow("no")
            .writeFinalDecisionGenerateNotice("no")
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
    public void givenManualUploadNonDailyLivingAndOrMobilityScenarioAndExplictOutcomeNotSetAndIrrelevantParametersSet_thenReturnNull(String comparedRateDailyLiving, String comparedRateMobility) {

        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving)
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility)
                .build())
            .writeFinalDecisionGenerateNotice("no")
            .writeFinalDecisionIsDescriptorFlow("no")
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
    public void givenManualUploadDailyLivingAndOrMobilityScenarioAndIsRefusedAndIrrelevantParametersSet_thenSetDecisionUpheld(String comparedRateDailyLiving, String comparedRateMobility,
        String expectedOutcome) {

        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving)
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility)
                .build())
            .writeFinalDecisionIsDescriptorFlow("yes")
            .writeFinalDecisionGenerateNotice("no")
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
    public void givenManualUploadDailyLivingAndOrMobilityScenarioAndIsAllowedAndIrrelevantParametersSet_thenSetDecisionInFavourOfAppellant(String comparedRateDailyLiving, String comparedRateMobility,
        String expectedOutcome) {

        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving)
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility)
                .build())
            .writeFinalDecisionIsDescriptorFlow("yes")
            .writeFinalDecisionGenerateNotice("no")
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
    public void givenManualUploadDailyLivingAndOrMobilityScenarioAndExplictOutcomeNotSetAndIrrelevantParametersSet_thenReturnNull(String comparedRateDailyLiving, String comparedRateMobility) {

        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving)
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility).build())
            .writeFinalDecisionGenerateNotice("no")
            .writeFinalDecisionIsDescriptorFlow("yes")
            .build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNull(outcome);
    }
}
