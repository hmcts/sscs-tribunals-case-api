package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EsaPointsConditionTest {

    @Test
    public void testConditionsWhenLessThan15() {
        Assertions.assertFalse(EsaPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN.getPointsRequirementCondition().test(14));
        Assertions.assertTrue(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN.getPointsRequirementCondition().test(14));
    }

    @Test
    public void testConditionWhenEqualTo15() {
        Assertions.assertTrue(EsaPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN.getPointsRequirementCondition().test(15));
        Assertions.assertFalse(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN.getPointsRequirementCondition().test(15));
    }

    @Test
    public void testConditionWhenGreaterThan15() {
        Assertions.assertTrue(EsaPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN.getPointsRequirementCondition().test(16));
        Assertions.assertFalse(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN.getPointsRequirementCondition().test(16));
    }

    /**
     * Test the continuity of boundaries between point ranges for daily living conditions. (ie. this test will fail if there are any gaps, or overlap between the boundaries)
     */
    @Test
    public void testThatAtExactlyOnePointsConditionPassesForAllPossiblePointValues() {

        int minPoints = 0;
        int maxPoints = 100;
        for (int points = minPoints; points < maxPoints; points++) {
            int pointsConditionSatisfiedCount = 0;
            for (EsaPointsCondition esaPointsCondition : EsaPointsCondition.values()) {
                if (esaPointsCondition.getPointsRequirementCondition().test(points)) {
                    pointsConditionSatisfiedCount++;
                }
            }

            Assertions.assertEquals(1, pointsConditionSatisfiedCount, "Expected 1 condition to be satisfied for points:" + points + " but " + pointsConditionSatisfiedCount + " were satisfied");
        }
    }

    @Test
    public void testAllPointsConditionAttributesAreNotNull() {
        for (EsaPointsCondition esaPointsCondition : EsaPointsCondition.values()) {
            Assertions.assertNotNull(esaPointsCondition.getPointsRequirementCondition());
        }
    }
}
