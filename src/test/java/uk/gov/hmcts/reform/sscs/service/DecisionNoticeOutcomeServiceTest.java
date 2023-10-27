package uk.gov.hmcts.reform.sscs.service;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.NO;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.YES;

import java.io.IOException;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import uk.gov.hmcts.reform.sscs.ccd.domain.Outcome;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsFinalDecisionCaseData;
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
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionGenerateNotice(YES)
                .writeFinalDecisionIsDescriptorFlow("yes")
                .build())
            .build();

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
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionGenerateNotice(YES)
                .writeFinalDecisionAllowedOrRefused("refused")
                .writeFinalDecisionIsDescriptorFlow("yes")
                .build())
            .build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNotNull(outcome);

        assertEquals(expectedOutcome, outcome.getId());
    }


    @Test
    public void givenFinalDecisionComparedToDwpQuestionWithIncorrectValue_ThenReturnNull() {

        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher")
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion("something")
                .build())
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionGenerateNotice(YES)
                .writeFinalDecisionIsDescriptorFlow("yes")
                .build())
            .build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNull(outcome);
    }

    @Test
    public void givenExplicitAlternatePathRefusalFinalDecisionComparedToDwpQuestionWithIncorrectValue_ThenReturnNull() {

        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher")
                .pipWriteFinalDecisionComparedToDwpMobilityQuestion("something")
                .build())
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionAllowedOrRefused("refused")
                .writeFinalDecisionGenerateNotice(YES)
                .writeFinalDecisionIsDescriptorFlow("yes")
                .build())
            .build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNull(outcome);
    }

    @Test
    public void givenFinalDecisionComparedToDwpQuestionWithNullValue_ThenReturnNull() {

        SscsCaseData caseData = SscsCaseData.builder()
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionIsDescriptorFlow("yes")
                .writeFinalDecisionGenerateNotice(YES)
                .build())
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
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionGenerateNotice(YES)
                .writeFinalDecisionAllowedOrRefused("refused")
                .build())
            .build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNull(outcome);
    }

    @Test
    public void givenExplicitAlternatePathRefusalFinalDecisionComparedToDwpQuestionWithNullValue_ThenReturnNull() {

        SscsCaseData caseData = SscsCaseData.builder()
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionIsDescriptorFlow("yes")
                .writeFinalDecisionAllowedOrRefused("refused")
                .writeFinalDecisionGenerateNotice(YES)
                .build())
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
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionIsDescriptorFlow("no")
                .writeFinalDecisionGenerateNotice(YES)
                .writeFinalDecisionAllowedOrRefused("refused")
                .build())
            .build();

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
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionIsDescriptorFlow("no")
                .writeFinalDecisionGenerateNotice(YES)
                .writeFinalDecisionAllowedOrRefused("allowed")
                .build())
            .build();

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
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionGenerateNotice(YES)
                .writeFinalDecisionIsDescriptorFlow("no")
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
    public void givenManualUploadNonDailyLivingAndOrMobilityScenarioAndIsRefusedAndIrrelevantParametersSet_thenSetDecisionUpheld(String comparedRateDailyLiving, String comparedRateMobility,
        String expectedOutcome) {

        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving)
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility)
                .build())
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionIsDescriptorFlow("no")
                .writeFinalDecisionGenerateNotice(NO)
                .writeFinalDecisionAllowedOrRefused("refused")
                .build())
            .build();

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
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionIsDescriptorFlow("no")
                .writeFinalDecisionGenerateNotice(NO)
                .writeFinalDecisionAllowedOrRefused("allowed")
                .build())
            .build();

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
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionGenerateNotice(NO)
                .writeFinalDecisionIsDescriptorFlow("no")
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
    public void givenManualUploadDailyLivingAndOrMobilityScenarioAndIsRefusedAndIrrelevantParametersSet_thenSetDecisionUpheld(String comparedRateDailyLiving, String comparedRateMobility,
        String expectedOutcome) {

        SscsCaseData caseData = SscsCaseData.builder().pipSscsCaseData(SscsPipCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion(comparedRateDailyLiving)
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility)
                .build())
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionIsDescriptorFlow("yes")
                .writeFinalDecisionGenerateNotice(NO)
                .writeFinalDecisionAllowedOrRefused("refused")
                .build())
            .build();

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
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionIsDescriptorFlow("yes")
                .writeFinalDecisionGenerateNotice(NO)
                .writeFinalDecisionAllowedOrRefused("allowed")
                .build())
            .build();

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
            .finalDecisionCaseData(SscsFinalDecisionCaseData.builder()
                .writeFinalDecisionGenerateNotice(NO)
                .writeFinalDecisionIsDescriptorFlow("yes")
                .build())
            .build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNull(outcome);
    }
}
