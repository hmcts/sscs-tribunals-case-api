package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import static org.mockito.MockitoAnnotations.openMocks;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import junitparams.JUnitParamsRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsPipCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityAnswer;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;

@RunWith(JUnitParamsRunner.class)
public class PipPointsConditionTest {

    @Mock
    private SscsPipCaseData sscsPipCaseData;

    @Mock
    private DecisionNoticeQuestionService decisionNoticeQuestionService;

    @Before
    public void setUp() throws IOException {
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

    @Test
    public void testOptionalErrorMessageIsEmptyWhenQuestionFullyAnsweredAndPointsMatch() {

        ActivityAnswer preparingFoodAnswer = ActivityAnswer.builder().activityAnswerPoints(8).activityAnswerValue("Answer Text").activityAnswerLetter("f").activityAnswerNumber("1").build();
        Optional<ActivityAnswer> answer = Optional.of(preparingFoodAnswer);
        PipPointsCondition pointsCondition = PipPointsCondition.DAILY_LIVING_STANDARD;
        List<String> answers = Arrays.asList("preparingFood");

        SscsCaseData caseData = SscsCaseData.builder()
                .pipSscsCaseData(SscsPipCaseData.builder()
            .pipWriteFinalDecisionDailyLivingActivitiesQuestion(answers).build()).build();

        Mockito.when(decisionNoticeQuestionService.getAnswerForActivityQuestionKey(caseData, "preparingFood")).thenReturn(answer);

        Optional<String> optionalErrorMessage = pointsCondition
            .getOptionalErrorMessage(decisionNoticeQuestionService, caseData);

        Assert.assertFalse(optionalErrorMessage.isPresent());

    }

    @Test
    public void testOptionalErrorMessageIsPresentWhenQuestionFullyAnsweredWhenPointsDontMatch() {

        ActivityAnswer preparingFoodAnswer = ActivityAnswer.builder().activityAnswerPoints(8).activityAnswerValue("Answer Text").activityAnswerLetter("f").activityAnswerNumber("1").build();
        Optional<ActivityAnswer> answer = Optional.of(preparingFoodAnswer);
        PipPointsCondition pointsCondition = PipPointsCondition.DAILY_LIVING_ENHANCED;
        List<String> answers = Arrays.asList("preparingFood");

        SscsCaseData caseData = SscsCaseData.builder()
                .pipSscsCaseData(SscsPipCaseData.builder()
            .pipWriteFinalDecisionDailyLivingActivitiesQuestion(answers).build()).build();

        Mockito.when(decisionNoticeQuestionService.getAnswerForActivityQuestionKey(caseData, "preparingFood")).thenReturn(answer);

        Optional<String> optionalErrorMessage = pointsCondition
            .getOptionalErrorMessage(this.decisionNoticeQuestionService,
                SscsCaseData.builder()
                        .pipSscsCaseData(SscsPipCaseData.builder()
                    .pipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f")
                    .pipWriteFinalDecisionDailyLivingActivitiesQuestion(answers).build()).build());

        Assert.assertTrue(optionalErrorMessage.isPresent());
        Assert.assertEquals(pointsCondition.getErrorMessage(), optionalErrorMessage.get());
        Assert.assertEquals("You have previously selected an enhanced rate award "
            + "for Daily Living. The points awarded don't match. "
            + "Please review your previous selection.", optionalErrorMessage.get());
    }

    @Test
    public void testOptionalErrorMessageIsPresentWhenPointsDontMatchWhenTopLevelQuestionAnsweredAndBottomLevelQuestionNotAnswered() {

        ActivityAnswer preparingFoodAnswer = ActivityAnswer.builder().activityAnswerPoints(8).activityAnswerValue("Answer Text").activityAnswerLetter("f").activityAnswerNumber("1").build();
        Optional<ActivityAnswer> answer = Optional.of(preparingFoodAnswer);
        List<String> answers = Arrays.asList("preparingFood");

        SscsCaseData caseData = SscsCaseData.builder()
                .pipSscsCaseData(SscsPipCaseData.builder()
            .pipWriteFinalDecisionDailyLivingActivitiesQuestion(answers).build()).build();

        Mockito.when(decisionNoticeQuestionService.getAnswerForActivityQuestionKey(caseData, "preparingFood")).thenReturn(Optional.empty());

        PipPointsCondition pointsCondition = PipPointsCondition.DAILY_LIVING_STANDARD;

        Optional<String> optionalErrorMessage = pointsCondition
            .getOptionalErrorMessage(this.decisionNoticeQuestionService,
                SscsCaseData.builder()
                        .pipSscsCaseData(SscsPipCaseData.builder()
                    .pipWriteFinalDecisionDailyLivingActivitiesQuestion(answers).build()).build());

        Assert.assertTrue(optionalErrorMessage.isPresent());
        Assert.assertEquals(pointsCondition.getErrorMessage(), optionalErrorMessage.get());
        Assert.assertEquals("You have previously selected a standard rate award "
            + "for Daily Living. The points awarded don't match. "
            + "Please review your previous selection.", optionalErrorMessage.get());
    }

    @Test
    public void testOptionalErrorMessageIsNotPresentWhenPointsMatchWhenTopLevelQuestionAnsweredAndBottomLevelQuestionNotAnswered() {

        ActivityAnswer preparingFoodAnswer = ActivityAnswer.builder().activityAnswerPoints(8).activityAnswerValue("Answer Text").activityAnswerLetter("f").activityAnswerNumber("1").build();
        Optional<ActivityAnswer> answer = Optional.of(preparingFoodAnswer);
        List<String> answers = Arrays.asList("preparingFood", "someothercategory");

        SscsCaseData caseData = SscsCaseData.builder()
            .pipSscsCaseData(SscsPipCaseData.builder()
            .pipWriteFinalDecisionDailyLivingActivitiesQuestion(answers).build()).build();

        Mockito.when(decisionNoticeQuestionService.getAnswerForActivityQuestionKey(caseData, "preparingFood")).thenReturn(Optional.empty());
        Mockito.when(decisionNoticeQuestionService.getAnswerForActivityQuestionKey(caseData, "preparingFood")).thenReturn(answer);


        PipPointsCondition pointsCondition = PipPointsCondition.DAILY_LIVING_STANDARD;


        Optional<String> optionalErrorMessage = pointsCondition
            .getOptionalErrorMessage(this.decisionNoticeQuestionService, caseData);

        Assert.assertFalse(optionalErrorMessage.isPresent());

    }

    @Test
    public void testOptionalErrorMessageIsEmptyWhenTopLevelQuestionNotAnsweredAndBottomLevelQuestionAnswered() {

        ActivityAnswer preparingFoodAnswer = ActivityAnswer.builder().activityAnswerPoints(8).activityAnswerValue("Answer Text").activityAnswerLetter("f").activityAnswerNumber("1").build();
        Optional<ActivityAnswer> answer = Optional.of(preparingFoodAnswer);

        List<String> answers = Arrays.asList();

        SscsCaseData caseData = SscsCaseData.builder()
                .pipSscsCaseData(SscsPipCaseData.builder()
            .pipWriteFinalDecisionDailyLivingActivitiesQuestion(answers).build()).build();

        Mockito.when(decisionNoticeQuestionService.getAnswerForActivityQuestionKey(caseData, "preparingFood")).thenReturn(answer);

        PipPointsCondition pointsCondition = PipPointsCondition.DAILY_LIVING_ENHANCED;
        Optional<String> optionalErrorMessage = pointsCondition
            .getOptionalErrorMessage(this.decisionNoticeQuestionService,
                SscsCaseData.builder()
                        .pipSscsCaseData(SscsPipCaseData.builder()
                    .pipWriteFinalDecisionPreparingFoodQuestion("preparingFood1f")
                    .pipWriteFinalDecisionDailyLivingActivitiesQuestion(answers).build()).build());

        Assert.assertTrue(optionalErrorMessage.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetStandardErrorMessage_WhenNotConsideredDailyLiving() {
        PipPointsCondition.getStandardErrorMessage(AwardType.NOT_CONSIDERED, PipActivityType.DAILY_LIVING);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetStandardErrorMessage_WhenNotConsideredMobility() {
        PipPointsCondition.getStandardErrorMessage(AwardType.NOT_CONSIDERED, PipActivityType.MOBILITY);
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

        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionDailyLivingQuestion()).thenReturn("noAward");
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionMobilityQuestion()).thenReturn("noAward");

        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();


        for (int dailyLivingPoints = 0; dailyLivingPoints < maxPoints; dailyLivingPoints++) {
            for (int mobilityPoints = 0; mobilityPoints < maxPoints; mobilityPoints++) {
                int conditionPasses = 0;
                int conditionFails = 0;

                for (PipPointsCondition pipPointsCondition : PipPointsCondition.values()) {
                    if (pipPointsCondition.isApplicable(decisionNoticeQuestionService, sscsCaseData)) {
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

        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionDailyLivingQuestion()).thenReturn("noAward");
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionMobilityQuestion()).thenReturn("standardRate");

        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();

        for (int dailyLivingPoints = 0; dailyLivingPoints < maxPoints; dailyLivingPoints++) {
            for (int mobilityPoints = 0; mobilityPoints < maxPoints; mobilityPoints++) {
                int conditionPasses = 0;
                int conditionFails = 0;

                for (PipPointsCondition pipPointsCondition : PipPointsCondition.values()) {
                    if (pipPointsCondition.isApplicable(decisionNoticeQuestionService, sscsCaseData)) {
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

        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();

        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionDailyLivingQuestion()).thenReturn("noAward");
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionMobilityQuestion()).thenReturn("enhancedRate");

        for (int dailyLivingPoints = 0; dailyLivingPoints < maxPoints; dailyLivingPoints++) {
            for (int mobilityPoints = 0; mobilityPoints < maxPoints; mobilityPoints++) {
                int conditionPasses = 0;
                int conditionFails = 0;

                for (PipPointsCondition pipPointsCondition : PipPointsCondition.values()) {
                    if (pipPointsCondition.isApplicable(decisionNoticeQuestionService, sscsCaseData)) {
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

        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();

        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionDailyLivingQuestion()).thenReturn("standardRate");
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionMobilityQuestion()).thenReturn("noAward");

        for (int dailyLivingPoints = 0; dailyLivingPoints < maxPoints; dailyLivingPoints++) {
            for (int mobilityPoints = 0; mobilityPoints < maxPoints; mobilityPoints++) {
                int conditionPasses = 0;
                int conditionFails = 0;

                for (PipPointsCondition pipPointsCondition : PipPointsCondition.values()) {
                    if (pipPointsCondition.isApplicable(decisionNoticeQuestionService, sscsCaseData)) {
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

        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();

        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionDailyLivingQuestion()).thenReturn("standardRate");
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionMobilityQuestion()).thenReturn("standardRate");

        for (int dailyLivingPoints = 0; dailyLivingPoints < maxPoints; dailyLivingPoints++) {
            for (int mobilityPoints = 0; mobilityPoints < maxPoints; mobilityPoints++) {
                int conditionPasses = 0;
                int conditionFails = 0;

                for (PipPointsCondition pipPointsCondition : PipPointsCondition.values()) {
                    if (pipPointsCondition.isApplicable(decisionNoticeQuestionService, sscsCaseData)) {
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

        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();

        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionDailyLivingQuestion()).thenReturn("standardRate");
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionMobilityQuestion()).thenReturn("enhancedRate");

        for (int dailyLivingPoints = 0; dailyLivingPoints < maxPoints; dailyLivingPoints++) {
            for (int mobilityPoints = 0; mobilityPoints < maxPoints; mobilityPoints++) {
                int conditionPasses = 0;
                int conditionFails = 0;

                for (PipPointsCondition pipPointsCondition : PipPointsCondition.values()) {
                    if (pipPointsCondition.isApplicable(decisionNoticeQuestionService, sscsCaseData)) {
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

        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();

        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionDailyLivingQuestion()).thenReturn("enhancedRate");
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionMobilityQuestion()).thenReturn("noAward");

        for (int dailyLivingPoints = 0; dailyLivingPoints < maxPoints; dailyLivingPoints++) {
            for (int mobilityPoints = 0; mobilityPoints < maxPoints; mobilityPoints++) {
                int conditionPasses = 0;
                int conditionFails = 0;

                for (PipPointsCondition pipPointsCondition : PipPointsCondition.values()) {
                    if (pipPointsCondition.isApplicable(decisionNoticeQuestionService, sscsCaseData)) {
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

        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();

        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionDailyLivingQuestion()).thenReturn("enhancedRate");
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionMobilityQuestion()).thenReturn("standardRate");

        for (int dailyLivingPoints = 0; dailyLivingPoints < maxPoints; dailyLivingPoints++) {
            for (int mobilityPoints = 0; mobilityPoints < maxPoints; mobilityPoints++) {
                int conditionPasses = 0;
                int conditionFails = 0;

                for (PipPointsCondition pipPointsCondition : PipPointsCondition.values()) {
                    if (pipPointsCondition.isApplicable(decisionNoticeQuestionService, sscsCaseData)) {
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

        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionDailyLivingQuestion()).thenReturn("enhancedRate");
        Mockito.when(sscsPipCaseData.getPipWriteFinalDecisionMobilityQuestion()).thenReturn("enhancedRate");

        SscsCaseData sscsCaseData = SscsCaseData.builder().pipSscsCaseData(sscsPipCaseData).build();

        for (int dailyLivingPoints = 0; dailyLivingPoints < maxPoints; dailyLivingPoints++) {
            for (int mobilityPoints = 0; mobilityPoints < maxPoints; mobilityPoints++) {
                int conditionPasses = 0;
                int conditionFails = 0;

                for (PipPointsCondition pipPointsCondition : PipPointsCondition.values()) {
                    if (pipPointsCondition.isApplicable(decisionNoticeQuestionService, sscsCaseData)) {
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
            Assert.assertNotNull(pipPointsCondition.getAnswersExtractor());
            Assert.assertNotNull(pipPointsCondition.getEnumClass());
            Assert.assertEquals(PipPointsCondition.class, pipPointsCondition.getEnumClass());
            Assert.assertNotNull(pipPointsCondition.awardType);
        }
    }

}
