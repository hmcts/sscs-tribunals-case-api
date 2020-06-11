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
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion(comparedRateMobility).build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNotNull(outcome);

        assertEquals(expectedOutcome, outcome.getId());
    }


    @Test
    public void givenFinalDecisionComparedToDwpQuestionWithIncorrectValue_ThenReturnNull() {

        SscsCaseData caseData = SscsCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher")
            .pipWriteFinalDecisionComparedToDwpMobilityQuestion("something").build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNull(outcome);
    }

    @Test
    public void givenFinalDecisionComparedToDwpQuestionWithNullValue_ThenReturnNull() {

        SscsCaseData caseData = SscsCaseData.builder().pipWriteFinalDecisionComparedToDwpDailyLivingQuestion("higher")
            .build();

        Outcome outcome = service.determineOutcome(caseData);

        Assert.assertNull(outcome);

    }
}
