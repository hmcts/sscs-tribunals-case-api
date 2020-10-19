package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import static org.mockito.MockitoAnnotations.openMocks;

import junitparams.JUnitParamsRunner;
import junitparams.NamedParameters;
import junitparams.Parameters;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class EsaPointsAndActivitiesConditionTest {

    @Mock
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        openMocks(this);
    }

    @NamedParameters("scheduleAndRegulationQuestionCombinations")
    @SuppressWarnings("unused")
    private Object[] scheduleAndRegulationQuestionCombinations() {
        return new Object[]{
            new Boolean[]{null, null, null},
            new Boolean[]{null, null, false},
            new Boolean[]{null, null, true},
            new Boolean[]{null, false, null},
            new Boolean[]{null, false, false},
            new Boolean[]{null, false, true},
            new Boolean[]{null, true, null},
            new Boolean[]{null, true, false},
            new Boolean[]{null, true, true},
            new Boolean[]{false, null, null},
            new Boolean[]{false, null, false},
            new Boolean[]{false, null, true},
            new Boolean[]{false, false, null},
            new Boolean[]{false, false, false},
            new Boolean[]{false, false, true},
            new Boolean[]{false, true, null},
            new Boolean[]{false, true, false},
            new Boolean[]{false, true, true},
            new Boolean[]{true, null, null},
            new Boolean[]{true, null, false},
            new Boolean[]{true, null, true},
            new Boolean[]{true, false, null},
            new Boolean[]{true, false, false},
            new Boolean[]{true, false, true},
            new Boolean[]{true, true, null},
            new Boolean[]{true, true, false},
            new Boolean[]{true, true, true},
        };
    }

    private boolean isValidCombinationFromSelectSchedule3ActivitiesOnwards(Boolean schedule3ActivitiesSelected,
        Boolean doesRegulation35Apply) {

        if (schedule3ActivitiesSelected == null) {
            return false;
        } else {
            if (schedule3ActivitiesSelected.booleanValue()) {
                if (doesRegulation35Apply != null) {
                    return false;
                }
            } else {
                if (doesRegulation35Apply == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isValidCombination(int points, Boolean doesRegulation29Apply, Boolean schedule3ActivitiesSelected,
        Boolean doesRegulation35Apply) {

        if (points < 15) {
            if (doesRegulation29Apply == null) {
                return false;
            } else {
                if (!doesRegulation29Apply.booleanValue()) {
                    if (schedule3ActivitiesSelected != null || doesRegulation35Apply != null) {
                        return false;
                    } else {
                        return true;
                    }
                } else {
                    return isValidCombinationFromSelectSchedule3ActivitiesOnwards(schedule3ActivitiesSelected, doesRegulation35Apply);
                }
            }
        } else {
            if (doesRegulation29Apply != null) {
                return false;
            }
            return isValidCombinationFromSelectSchedule3ActivitiesOnwards(schedule3ActivitiesSelected, doesRegulation35Apply);
        }
    }

    /**
     * Test the continuity of boundaries between point ranges and regulation and schedule answers. (ie. this test will fail if there are any gaps, or overlap between the boundaries)
     */
    @Test
    @Parameters(named = "scheduleAndRegulationQuestionCombinations")
    public void testThatAtExactlyOneConditionPassesForAllPossiblePointAndActivityCombinations(
        Boolean doesRegulation29Apply, Boolean schedule3ActivitiesSelected,
        Boolean doesRegulation35Apply) {

        int minPoints = 14;
        int maxPoints = 16;
        for (int points = minPoints; points <= maxPoints; points++) {

            boolean validCombination = isValidCombination(points, doesRegulation29Apply,
                schedule3ActivitiesSelected, doesRegulation35Apply);

            int pointsConditionSatisfiedCount = 0;
            for (EsaPointsAndActivitiesCondition esaPointsCondition : EsaPointsAndActivitiesCondition.values()) {

                try {
                    if (esaPointsCondition.isSatisified(points, doesRegulation29Apply, schedule3ActivitiesSelected,
                        doesRegulation35Apply)) {
                        pointsConditionSatisfiedCount++;
                    }
                } catch (IllegalStateException e) {
                   // Do not increment points condition satisified count if it's not a valid combination
                }
            }
            if (validCombination) {
                Assert.assertEquals(
                    "Expected 1 condition to be satisfied for points:" + points + ":" + doesRegulation29Apply + ":" + schedule3ActivitiesSelected + ":" + doesRegulation35Apply + " but "
                        + pointsConditionSatisfiedCount + " were satisfied",
                    1, pointsConditionSatisfiedCount);
            } else {
                Assert.assertEquals(
                    "Expected 0 conditions to be satisfied for points:" + points + ":" + doesRegulation29Apply + ":" + schedule3ActivitiesSelected + ":" + doesRegulation35Apply + " but "
                        + pointsConditionSatisfiedCount + " were satisfied",
                    0, pointsConditionSatisfiedCount);
            }
        }
    }

    /**
     * We have separate tests above to ensure that only a single PointsCondition per activity type exists given an activity type and points - this method returns that condition.
     */
    private EsaPointsAndActivitiesCondition getTheSinglePassingPointsConditionForSubmittedActivitiesAndPoints(
        Boolean doesRegulation29Apply, Boolean schedule3ActivitiesSelected,
        Boolean doesRegulation35Apply, int points) {
        for (EsaPointsAndActivitiesCondition esaPointsAndActivitiesCondition : EsaPointsAndActivitiesCondition.values()) {

            if (esaPointsAndActivitiesCondition.isSatisified(points, doesRegulation29Apply, schedule3ActivitiesSelected,
                doesRegulation35Apply)) {
                if (esaPointsAndActivitiesCondition.getPointsCondition().getPointsRequirementCondition().test(points)) {
                    return esaPointsAndActivitiesCondition;
                }
            }
        }
        throw new IllegalStateException("No points condition found for points:" + points + " and " + doesRegulation29Apply + ":" + schedule3ActivitiesSelected + ":" + doesRegulation35Apply);
    }

    @Test
    public void testAllPointsConditionAttributesAreNotNull() {
        for (EsaPointsAndActivitiesCondition esaPointsCondition : EsaPointsAndActivitiesCondition.values()) {
            Assert.assertNotNull(esaPointsCondition.getPointsCondition());

            //Assert.assertNotNull(pipPointsCondition.getErrorMessage());
            //Assert.assertNotNull(pipPointsCondition.getActivityType());
            //Assert.assertNotNull(pipPointsCondition.getPointsRequirementCondition());
            //Assert.assertNotNull(pipPointsCondition.awardType);
        }
    }
}
