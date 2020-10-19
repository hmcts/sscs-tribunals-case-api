package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import static org.mockito.MockitoAnnotations.openMocks;

import junitparams.JUnitParamsRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityType;

@RunWith(JUnitParamsRunner.class)
public class PipPointsConditionTest {

    @Mock
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
    }

    /**
     * Test the continuity of boundaries between point ranges for daily living conditions. (ie. this test will fail if there are any gaps, or overlap between the boundaries)
     */
    @Test
    public void testThatAtExactlyOneDailyLivingPointsConditionPassesForAllPossiblePointValues() {

        int minPoints = 0;
        int maxPoints = 100;
        for (int points = minPoints; points < maxPoints; points++) {
            int pointsConditionSatisfiedCount = 0;
            for (PipPointsCondition pipPointsCondition : PipPointsCondition.values()) {
                if (pipPointsCondition.getActivityType().equals(PipActivityType.DAILY_LIVING)) {
                    if (pipPointsCondition.getPointsRequirementCondition().test(points)) {
                        pointsConditionSatisfiedCount++;
                    }
                }
            }

            Assert.assertEquals("Expected 1 condition to be satisfied for points:" + points + " for activity type DAILY_LIVING but " + pointsConditionSatisfiedCount + " were satisfied",
                1, pointsConditionSatisfiedCount);
        }
    }


    @Test(expected = IllegalArgumentException.class)
    public void testGetStandardErrorMessage_WhenNotConsideredDailyLiving() {
        PipPointsCondition.getStandardErrorMessage(PipAwardType.NOT_CONSIDERED, PipActivityType.DAILY_LIVING);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetStandardErrorMessage_WhenNotConsideredMobility() {
        PipPointsCondition.getStandardErrorMessage(PipAwardType.NOT_CONSIDERED, PipActivityType.MOBILITY);
    }

    /**
     * Test the continuity of boundaries between point ranges for mobility conditions. * (ie. this test will fail if there are any gaps, or overlap between the boundaries)
     */
    @Test
    public void testThatAtExactlyOneMobilityConditionPassesForPossiblePointValues() {

        int maxPoints = 100;

        for (int points = 0; points < maxPoints; points++) {
            int pointsConditionSatisfiedCount = 0;
            for (PipPointsCondition pipPointsCondition : PipPointsCondition.values()) {
                if (pipPointsCondition.getActivityType().equals(PipActivityType.MOBILITY)) {
                    if (pipPointsCondition.getPointsRequirementCondition().test(points)) {
                        pointsConditionSatisfiedCount++;
                    }
                }
            }
            Assert.assertEquals("Expected 1 condition to be satisfied for points:" + points + " for activity type MOBILITY but " + pointsConditionSatisfiedCount + " were satisfied",
                1, pointsConditionSatisfiedCount);
        }
    }

    /**
     * We have separate tests above to ensure that only a single PointsCondition per activity type exists given an activity type and points - this method returns that condition.
     */
    private PipPointsCondition getTheSinglePassingPointsConditionForSubmittedPoints(ActivityType activityType, int points) {
        for (PipPointsCondition pipPointsCondition : PipPointsCondition.values()) {
            if (pipPointsCondition.getActivityType().equals(activityType)) {
                if (pipPointsCondition.getPointsRequirementCondition().test(points)) {
                    return pipPointsCondition;
                }
            }
        }
        throw new IllegalStateException("No points condition found for points:" + points + " and " + activityType);
    }

    @Test
    public void testDailyLivingNoAwardAndMobilityNoAward() {

        int maxPoints = 100;

        Mockito.when(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion()).thenReturn("noAward");
        Mockito.when(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion()).thenReturn("noAward");

        for (int dailyLivingPoints = 0; dailyLivingPoints < maxPoints; dailyLivingPoints++) {
            for (int mobilityPoints = 0; mobilityPoints < maxPoints; mobilityPoints++) {
                int conditionPasses = 0;
                int conditionFails = 0;

                for (PipPointsCondition pipPointsCondition : PipPointsCondition.values()) {
                    if (pipPointsCondition.isApplicable(sscsCaseData)) {
                        if (pipPointsCondition.getActivityType() == PipActivityType.DAILY_LIVING) {
                            if (pipPointsCondition.getPointsRequirementCondition().test(dailyLivingPoints)) {
                                conditionPasses++;
                            } else {
                                conditionFails++;
                            }
                        }
                        if (pipPointsCondition.getActivityType() == PipActivityType.MOBILITY) {
                            if (pipPointsCondition.getPointsRequirementCondition().test(mobilityPoints)) {
                                conditionPasses++;
                            } else {
                                conditionFails++;
                            }
                        }
                    }
                }

                if (dailyLivingPoints < 8 && mobilityPoints < 8) {
                    Assert.assertEquals(2, conditionPasses);
                    Assert.assertEquals(0, conditionFails);
                } else if (dailyLivingPoints < 8 && mobilityPoints >= 8) {
                    Assert.assertEquals(1, conditionFails);
                    Assert.assertEquals(1, conditionPasses);
                } else if (dailyLivingPoints >= 8 && mobilityPoints < 8) {
                    Assert.assertEquals(1, conditionFails);
                    Assert.assertEquals(1, conditionPasses);
                } else if (dailyLivingPoints >= 8 && mobilityPoints >= 8) {
                    Assert.assertEquals(2, conditionFails);
                    Assert.assertEquals(0, conditionPasses);
                } else {
                    throw new IllegalStateException("Points condition not covered - daily living = " + dailyLivingPoints + " and mobility = " + mobilityPoints);
                }
            }
        }
    }

    @Test
    public void testDailyLivingNoAwardAndMobilityStandardRate() {

        int maxPoints = 100;

        Mockito.when(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion()).thenReturn("noAward");
        Mockito.when(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion()).thenReturn("standardRate");

        for (int dailyLivingPoints = 0; dailyLivingPoints < maxPoints; dailyLivingPoints++) {
            for (int mobilityPoints = 0; mobilityPoints < maxPoints; mobilityPoints++) {
                int conditionPasses = 0;
                int conditionFails = 0;

                for (PipPointsCondition pipPointsCondition : PipPointsCondition.values()) {
                    if (pipPointsCondition.isApplicable(sscsCaseData)) {
                        if (pipPointsCondition.getActivityType() == PipActivityType.DAILY_LIVING) {
                            if (pipPointsCondition.getPointsRequirementCondition().test(dailyLivingPoints)) {
                                conditionPasses++;
                            } else {
                                conditionFails++;
                            }
                        }
                        if (pipPointsCondition.getActivityType() == PipActivityType.MOBILITY) {
                            if (pipPointsCondition.getPointsRequirementCondition().test(mobilityPoints)) {
                                conditionPasses++;
                            } else {
                                conditionFails++;
                            }
                        }
                    }
                }
                if (dailyLivingPoints < 8 && mobilityPoints < 8) {
                    Assert.assertEquals(1, conditionPasses);
                    Assert.assertEquals(1, conditionFails);
                } else if (dailyLivingPoints < 8 && mobilityPoints < 12) {
                    Assert.assertEquals(0, conditionFails);
                    Assert.assertEquals(2, conditionPasses);
                } else if (dailyLivingPoints < 8 && mobilityPoints >= 12) {
                    Assert.assertEquals(1, conditionFails);
                    Assert.assertEquals(1, conditionPasses);
                } else if (dailyLivingPoints >= 8 && mobilityPoints < 8) {
                    Assert.assertEquals(2, conditionFails);
                    Assert.assertEquals(0, conditionPasses);
                } else if (dailyLivingPoints >= 8 && mobilityPoints < 12) {
                    Assert.assertEquals(1, conditionFails);
                    Assert.assertEquals(1, conditionPasses);
                } else if (dailyLivingPoints >= 8 && mobilityPoints >= 12) {
                    Assert.assertEquals(2, conditionFails);
                    Assert.assertEquals(0, conditionPasses);
                } else {
                    throw new IllegalStateException("Points condition not covered - daily living = " + dailyLivingPoints + " and mobility = " + mobilityPoints);
                }
            }
        }
    }

    @Test
    public void testDailyLivingNoAwardAndMobilityEnhancedRate() {

        int maxPoints = 100;

        Mockito.when(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion()).thenReturn("noAward");
        Mockito.when(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion()).thenReturn("enhancedRate");

        for (int dailyLivingPoints = 0; dailyLivingPoints < maxPoints; dailyLivingPoints++) {
            for (int mobilityPoints = 0; mobilityPoints < maxPoints; mobilityPoints++) {
                int conditionPasses = 0;
                int conditionFails = 0;

                for (PipPointsCondition pipPointsCondition : PipPointsCondition.values()) {
                    if (pipPointsCondition.isApplicable(sscsCaseData)) {
                        if (pipPointsCondition.getActivityType() == PipActivityType.DAILY_LIVING) {
                            if (pipPointsCondition.getPointsRequirementCondition().test(dailyLivingPoints)) {
                                conditionPasses++;
                            } else {
                                conditionFails++;
                            }
                        }
                        if (pipPointsCondition.getActivityType() == PipActivityType.MOBILITY) {
                            if (pipPointsCondition.getPointsRequirementCondition().test(mobilityPoints)) {
                                conditionPasses++;
                            } else {
                                conditionFails++;
                            }
                        }
                    }
                }
                if (dailyLivingPoints < 8 && mobilityPoints < 8) {
                    Assert.assertEquals(1, conditionPasses);
                    Assert.assertEquals(1, conditionFails);
                } else if (dailyLivingPoints < 8 && mobilityPoints < 12) {
                    Assert.assertEquals(1, conditionFails);
                    Assert.assertEquals(1, conditionPasses);
                } else if (dailyLivingPoints < 8 && mobilityPoints >= 12) {
                    Assert.assertEquals(0, conditionFails);
                    Assert.assertEquals(2, conditionPasses);
                } else if (dailyLivingPoints >= 8 && mobilityPoints < 8) {
                    Assert.assertEquals(2, conditionFails);
                    Assert.assertEquals(0, conditionPasses);
                } else if (dailyLivingPoints >= 8 && mobilityPoints < 12) {
                    Assert.assertEquals(2, conditionFails);
                    Assert.assertEquals(0, conditionPasses);
                } else if (dailyLivingPoints >= 8 && mobilityPoints >= 12) {
                    Assert.assertEquals(1, conditionFails);
                    Assert.assertEquals(1, conditionPasses);
                } else {
                    throw new IllegalStateException("Points condition not covered - daily living = " + dailyLivingPoints + " and mobility = " + mobilityPoints);
                }
            }
        }
    }

    @Test
    public void testDailyLivingStandardRateAndMobilityNoAward() {

        int maxPoints = 100;

        Mockito.when(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion()).thenReturn("standardRate");
        Mockito.when(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion()).thenReturn("noAward");

        for (int dailyLivingPoints = 0; dailyLivingPoints < maxPoints; dailyLivingPoints++) {
            for (int mobilityPoints = 0; mobilityPoints < maxPoints; mobilityPoints++) {
                int conditionPasses = 0;
                int conditionFails = 0;

                for (PipPointsCondition pipPointsCondition : PipPointsCondition.values()) {
                    if (pipPointsCondition.isApplicable(sscsCaseData)) {
                        if (pipPointsCondition.getActivityType() == PipActivityType.DAILY_LIVING) {
                            if (pipPointsCondition.getPointsRequirementCondition().test(dailyLivingPoints)) {
                                conditionPasses++;
                            } else {
                                conditionFails++;
                            }
                        }
                        if (pipPointsCondition.getActivityType() == PipActivityType.MOBILITY) {
                            if (pipPointsCondition.getPointsRequirementCondition().test(mobilityPoints)) {
                                conditionPasses++;
                            } else {
                                conditionFails++;
                            }
                        }
                    }
                }

                if (dailyLivingPoints < 8 && mobilityPoints < 8) {
                    Assert.assertEquals(1, conditionPasses);
                    Assert.assertEquals(1, conditionFails);
                } else if (dailyLivingPoints < 8 && mobilityPoints >= 8) {
                    Assert.assertEquals(2, conditionFails);
                    Assert.assertEquals(0, conditionPasses);
                } else if (dailyLivingPoints >= 8 && dailyLivingPoints < 12 && mobilityPoints < 8) {
                    Assert.assertEquals(0, conditionFails);
                    Assert.assertEquals(2, conditionPasses);
                } else if (dailyLivingPoints >= 8 && dailyLivingPoints < 12 && mobilityPoints >= 8) {
                    Assert.assertEquals(1, conditionFails);
                    Assert.assertEquals(1, conditionPasses);
                } else if (dailyLivingPoints >= 12 && mobilityPoints < 8) {
                    Assert.assertEquals(1, conditionFails);
                    Assert.assertEquals(1, conditionPasses);
                } else if (dailyLivingPoints >= 12 && mobilityPoints >= 8) {
                    Assert.assertEquals(2, conditionFails);
                    Assert.assertEquals(0, conditionPasses);
                } else {
                    throw new IllegalStateException("Points condition not covered - daily living = " + dailyLivingPoints + " and mobility = " + mobilityPoints);
                }
            }
        }
    }

    @Test
    public void testDailyLivingStandardRateAndMobilityStandardRate() {

        int maxPoints = 100;

        Mockito.when(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion()).thenReturn("standardRate");
        Mockito.when(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion()).thenReturn("standardRate");

        for (int dailyLivingPoints = 0; dailyLivingPoints < maxPoints; dailyLivingPoints++) {
            for (int mobilityPoints = 0; mobilityPoints < maxPoints; mobilityPoints++) {
                int conditionPasses = 0;
                int conditionFails = 0;

                for (PipPointsCondition pipPointsCondition : PipPointsCondition.values()) {
                    if (pipPointsCondition.isApplicable(sscsCaseData)) {
                        if (pipPointsCondition.getActivityType() == PipActivityType.DAILY_LIVING) {
                            if (pipPointsCondition.getPointsRequirementCondition().test(dailyLivingPoints)) {
                                conditionPasses++;
                            } else {
                                conditionFails++;
                            }
                        }
                        if (pipPointsCondition.getActivityType() == PipActivityType.MOBILITY) {
                            if (pipPointsCondition.getPointsRequirementCondition().test(mobilityPoints)) {
                                conditionPasses++;
                            } else {
                                conditionFails++;
                            }
                        }
                    }
                }
                if (dailyLivingPoints < 8 && mobilityPoints < 8) {
                    Assert.assertEquals(0, conditionPasses);
                    Assert.assertEquals(2, conditionFails);
                } else if (dailyLivingPoints < 8 && mobilityPoints < 12) {
                    Assert.assertEquals(1, conditionFails);
                    Assert.assertEquals(1, conditionPasses);
                } else if (dailyLivingPoints < 8 && mobilityPoints >= 12) {
                    Assert.assertEquals(2, conditionFails);
                    Assert.assertEquals(0, conditionPasses);
                } else if (dailyLivingPoints >= 8 && dailyLivingPoints < 12 && mobilityPoints < 8) {
                    Assert.assertEquals(1, conditionFails);
                    Assert.assertEquals(1, conditionPasses);
                } else if (dailyLivingPoints >= 8 && dailyLivingPoints < 12 && mobilityPoints < 12) {
                    Assert.assertEquals(0, conditionFails);
                    Assert.assertEquals(2, conditionPasses);
                } else if (dailyLivingPoints >= 8 && dailyLivingPoints < 12 && mobilityPoints >= 12) {
                    Assert.assertEquals(1, conditionFails);
                    Assert.assertEquals(1, conditionPasses);
                } else if (dailyLivingPoints >= 12 && mobilityPoints < 8) {
                    Assert.assertEquals(2, conditionFails);
                    Assert.assertEquals(0, conditionPasses);
                } else if (dailyLivingPoints >= 12 && mobilityPoints < 12) {
                    Assert.assertEquals(1, conditionFails);
                    Assert.assertEquals(1, conditionPasses);
                } else if (dailyLivingPoints >= 12 && mobilityPoints >= 12) {
                    Assert.assertEquals(2, conditionFails);
                    Assert.assertEquals(0, conditionPasses);
                } else {
                    throw new IllegalStateException("Points condition not covered - daily living = " + dailyLivingPoints + " and mobility = " + mobilityPoints);
                }
            }
        }
    }

    @Test
    public void testDailyLivingStandardRateAndMobilityEnhancedRate() {

        int maxPoints = 100;

        Mockito.when(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion()).thenReturn("standardRate");
        Mockito.when(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion()).thenReturn("enhancedRate");

        for (int dailyLivingPoints = 0; dailyLivingPoints < maxPoints; dailyLivingPoints++) {
            for (int mobilityPoints = 0; mobilityPoints < maxPoints; mobilityPoints++) {
                int conditionPasses = 0;
                int conditionFails = 0;

                for (PipPointsCondition pipPointsCondition : PipPointsCondition.values()) {
                    if (pipPointsCondition.isApplicable(sscsCaseData)) {
                        if (pipPointsCondition.getActivityType() == PipActivityType.DAILY_LIVING) {
                            if (pipPointsCondition.getPointsRequirementCondition().test(dailyLivingPoints)) {
                                conditionPasses++;
                            } else {
                                conditionFails++;
                            }
                        }
                        if (pipPointsCondition.getActivityType() == PipActivityType.MOBILITY) {
                            if (pipPointsCondition.getPointsRequirementCondition().test(mobilityPoints)) {
                                conditionPasses++;
                            } else {
                                conditionFails++;
                            }
                        }
                    }
                }
                if (dailyLivingPoints < 8 && mobilityPoints < 8) {
                    Assert.assertEquals(0, conditionPasses);
                    Assert.assertEquals(2, conditionFails);
                } else if (dailyLivingPoints < 8 && mobilityPoints < 12) {
                    Assert.assertEquals(2, conditionFails);
                    Assert.assertEquals(0, conditionPasses);
                } else if (dailyLivingPoints < 8 && mobilityPoints >= 12) {
                    Assert.assertEquals(1, conditionFails);
                    Assert.assertEquals(1, conditionPasses);
                } else if (dailyLivingPoints >= 8 && dailyLivingPoints < 12 && mobilityPoints < 12) {
                    Assert.assertEquals(1, conditionFails);
                    Assert.assertEquals(1, conditionPasses);
                } else if (dailyLivingPoints >= 8 && dailyLivingPoints < 12 && mobilityPoints >= 12) {
                    Assert.assertEquals(0, conditionFails);
                    Assert.assertEquals(2, conditionPasses);
                } else if (dailyLivingPoints >= 12 && mobilityPoints < 12) {
                    Assert.assertEquals(2, conditionFails);
                    Assert.assertEquals(0, conditionPasses);
                } else if (dailyLivingPoints >= 12 && mobilityPoints >= 12) {
                    Assert.assertEquals(1, conditionFails);
                    Assert.assertEquals(1, conditionPasses);
                } else {
                    throw new IllegalStateException("Points condition not covered - daily living = " + dailyLivingPoints + " and mobility = " + mobilityPoints);
                }
            }
        }
    }

    @Test
    public void testDailyLivingEnhancedRateAndMobilityNoAward() {

        int maxPoints = 100;

        Mockito.when(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion()).thenReturn("enhancedRate");
        Mockito.when(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion()).thenReturn("noAward");

        for (int dailyLivingPoints = 0; dailyLivingPoints < maxPoints; dailyLivingPoints++) {
            for (int mobilityPoints = 0; mobilityPoints < maxPoints; mobilityPoints++) {
                int conditionPasses = 0;
                int conditionFails = 0;

                for (PipPointsCondition pipPointsCondition : PipPointsCondition.values()) {
                    if (pipPointsCondition.isApplicable(sscsCaseData)) {
                        if (pipPointsCondition.getActivityType() == PipActivityType.DAILY_LIVING) {
                            if (pipPointsCondition.getPointsRequirementCondition().test(dailyLivingPoints)) {
                                conditionPasses++;
                            } else {
                                conditionFails++;
                            }
                        }
                        if (pipPointsCondition.getActivityType() == PipActivityType.MOBILITY) {
                            if (pipPointsCondition.getPointsRequirementCondition().test(mobilityPoints)) {
                                conditionPasses++;
                            } else {
                                conditionFails++;
                            }
                        }
                    }
                }

                if (dailyLivingPoints < 12 && mobilityPoints < 8) {
                    Assert.assertEquals(1, conditionPasses);
                    Assert.assertEquals(1, conditionFails);
                } else if (dailyLivingPoints < 12 && mobilityPoints >= 8) {
                    Assert.assertEquals(2, conditionFails);
                    Assert.assertEquals(0, conditionPasses);
                } else if (dailyLivingPoints >= 12 && mobilityPoints < 8) {
                    Assert.assertEquals(0, conditionFails);
                    Assert.assertEquals(2, conditionPasses);
                } else if (dailyLivingPoints >= 12 && mobilityPoints >= 8) {
                    Assert.assertEquals(1, conditionFails);
                    Assert.assertEquals(1, conditionPasses);
                } else {
                    throw new IllegalStateException("Points condition not covered - daily living = " + dailyLivingPoints + " and mobility = " + mobilityPoints);
                }
            }
        }
    }

    @Test
    public void testDailyLivingEnhancedRateAndMobilityStandardRate() {

        int maxPoints = 100;

        Mockito.when(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion()).thenReturn("enhancedRate");
        Mockito.when(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion()).thenReturn("standardRate");

        for (int dailyLivingPoints = 0; dailyLivingPoints < maxPoints; dailyLivingPoints++) {
            for (int mobilityPoints = 0; mobilityPoints < maxPoints; mobilityPoints++) {
                int conditionPasses = 0;
                int conditionFails = 0;

                for (PipPointsCondition pipPointsCondition : PipPointsCondition.values()) {
                    if (pipPointsCondition.isApplicable(sscsCaseData)) {
                        if (pipPointsCondition.getActivityType() == PipActivityType.DAILY_LIVING) {
                            if (pipPointsCondition.getPointsRequirementCondition().test(dailyLivingPoints)) {
                                conditionPasses++;
                            } else {
                                conditionFails++;
                            }
                        }
                        if (pipPointsCondition.getActivityType() == PipActivityType.MOBILITY) {
                            if (pipPointsCondition.getPointsRequirementCondition().test(mobilityPoints)) {
                                conditionPasses++;
                            } else {
                                conditionFails++;
                            }
                        }
                    }
                }
                if (dailyLivingPoints < 12 && mobilityPoints < 8) {
                    Assert.assertEquals(0, conditionPasses);
                    Assert.assertEquals(2, conditionFails);
                } else if (dailyLivingPoints < 12 && mobilityPoints < 12) {
                    Assert.assertEquals(1, conditionFails);
                    Assert.assertEquals(1, conditionPasses);
                } else if (dailyLivingPoints < 12 && mobilityPoints >= 12) {
                    Assert.assertEquals(2, conditionFails);
                    Assert.assertEquals(0, conditionPasses);
                } else if (dailyLivingPoints >= 12 && mobilityPoints < 8) {
                    Assert.assertEquals(1, conditionFails);
                    Assert.assertEquals(1, conditionPasses);
                } else if (dailyLivingPoints >= 12 && mobilityPoints < 12) {
                    Assert.assertEquals(0, conditionFails);
                    Assert.assertEquals(2, conditionPasses);
                } else if (dailyLivingPoints >= 12 && mobilityPoints >= 12) {
                    Assert.assertEquals(1, conditionFails);
                    Assert.assertEquals(1, conditionPasses);
                } else {
                    throw new IllegalStateException("Points condition not covered - daily living = " + dailyLivingPoints + " and mobility = " + mobilityPoints);
                }

            }
        }
    }

    @Test
    public void testDailyLivingEnhancedRateAndMobilityEnhancedRate() {

        int maxPoints = 100;

        Mockito.when(sscsCaseData.getPipWriteFinalDecisionDailyLivingQuestion()).thenReturn("enhancedRate");
        Mockito.when(sscsCaseData.getPipWriteFinalDecisionMobilityQuestion()).thenReturn("enhancedRate");

        for (int dailyLivingPoints = 0; dailyLivingPoints < maxPoints; dailyLivingPoints++) {
            for (int mobilityPoints = 0; mobilityPoints < maxPoints; mobilityPoints++) {
                int conditionPasses = 0;
                int conditionFails = 0;

                for (PipPointsCondition pipPointsCondition : PipPointsCondition.values()) {
                    if (pipPointsCondition.isApplicable(sscsCaseData)) {
                        if (pipPointsCondition.getActivityType() == PipActivityType.DAILY_LIVING) {
                            if (pipPointsCondition.getPointsRequirementCondition().test(dailyLivingPoints)) {
                                conditionPasses++;
                            } else {
                                conditionFails++;
                            }
                        }
                        if (pipPointsCondition.getActivityType() == PipActivityType.MOBILITY) {
                            if (pipPointsCondition.getPointsRequirementCondition().test(mobilityPoints)) {
                                conditionPasses++;
                            } else {
                                conditionFails++;
                            }
                        }
                    }
                }
                if (dailyLivingPoints < 12 && mobilityPoints < 12) {
                    Assert.assertEquals(0, conditionPasses);
                    Assert.assertEquals(2, conditionFails);
                } else if (dailyLivingPoints < 12 && mobilityPoints >= 12) {
                    Assert.assertEquals(1, conditionFails);
                    Assert.assertEquals(1, conditionPasses);
                } else if (dailyLivingPoints >= 12 && mobilityPoints < 12) {
                    Assert.assertEquals(1, conditionFails);
                    Assert.assertEquals(1, conditionPasses);
                } else {
                    Assert.assertEquals(0, conditionFails);
                    Assert.assertEquals(2, conditionPasses);
                }
            }
        }
    }

    @Test
    public void testAllPointsConditionAttributesAreNotNull() {
        for (PipPointsCondition pipPointsCondition : PipPointsCondition.values()) {
            Assert.assertNotNull(pipPointsCondition.getErrorMessage());
            Assert.assertNotNull(pipPointsCondition.getActivityType());
            Assert.assertNotNull(pipPointsCondition.getPointsRequirementCondition());
            Assert.assertNotNull(pipPointsCondition.awardType);
        }
    }

}
