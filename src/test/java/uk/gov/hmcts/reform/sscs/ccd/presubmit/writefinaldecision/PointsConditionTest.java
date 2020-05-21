package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import static org.mockito.MockitoAnnotations.initMocks;

import junitparams.JUnitParamsRunner;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

@RunWith(JUnitParamsRunner.class)
public class PointsConditionTest {

    @Mock
    private SscsCaseData sscsCaseData;

    @Before
    public void setUp() {
        initMocks(this);
    }

    /**
     * Test the continuity of boundaries between point ranges for daily living conditions.
     */
    @Test
    public void testThatAtLeastOneDailyPointsConditionPassesForPossiblePointValues() {

        int minPoints = 0;
        int maxPoints = 100;

        for (int points = minPoints; points < maxPoints; points++) {
            boolean pointsConditionSatisfied = false;
            for (PointsCondition pointsCondition : PointsCondition.values()) {
                if (pointsCondition.getActivityType().equals(ActivityType.DAILY_LIVING)) {
                    if (pointsCondition.getPointsRequirementCondition().test(points)) {
                        pointsConditionSatisfied = true;
                    }
                }
            }
            Assert.assertTrue("No condition is satisfiable for points:" + points + " for activity type DAILY_LIVING", pointsConditionSatisfied);
        }
    }

    /**
     * Test the continuity of boundaries between point ranges for daily living conditions.
     */
    @Test
    public void testThatAtLeastOneMobilityConditionPassesForPossiblePointValues() {

        int minPoints = 0;
        int maxPoints = 100;

        for (int points = minPoints; points < maxPoints; points++) {
            boolean pointsConditionSatisfied = false;
            for (PointsCondition pointsCondition : PointsCondition.values()) {
                if (pointsCondition.getActivityType().equals(ActivityType.MOBILITY)) {
                    if (pointsCondition.getPointsRequirementCondition().test(points)) {
                        pointsConditionSatisfied = true;
                    }
                }
            }
            Assert.assertTrue("No condition is satisfiable for points:" + points + " for activity type MOBILITY", pointsConditionSatisfied);
        }
    }

    @Test
    public void testAllPointsConditionAttributesAreNotNull() {
        for (PointsCondition pointsCondition : PointsCondition.values()) {
            Assert.assertNotNull(pointsCondition.getErrorMessage());
            Assert.assertNotNull(pointsCondition.getActivityType());
            Assert.assertNotNull(pointsCondition.getPointsRequirementCondition());
            Assert.assertNotNull(pointsCondition.awardType);
        }
    }

}
