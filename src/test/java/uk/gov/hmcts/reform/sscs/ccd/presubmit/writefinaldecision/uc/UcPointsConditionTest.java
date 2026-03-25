package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class UcPointsConditionTest {

    @Test
    public void testConditionsWhenEqualToZero() {
        assertThat(UcPointsCondition.ZERO_POINTS.pointsRequirementCondition.test(0)).isTrue();
        assertThat(UcPointsCondition.POINTS_LESS_THAN_FIFTEEN.getPointsRequirementCondition().test(0)).isTrue();
        assertThat(UcPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN.getPointsRequirementCondition().test(0)).isFalse();
    }

    @Test
    public void testConditionsWhenLessThan15() {
        assertThat(UcPointsCondition.ZERO_POINTS.pointsRequirementCondition.test(14)).isFalse();
        assertThat(UcPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN.getPointsRequirementCondition().test(14)).isFalse();
        assertThat(UcPointsCondition.POINTS_LESS_THAN_FIFTEEN.getPointsRequirementCondition().test(14)).isTrue();
    }

    @Test
    public void testConditionWhenEqualTo15() {
        assertThat(UcPointsCondition.ZERO_POINTS.pointsRequirementCondition.test(15)).isFalse();
        assertThat(UcPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN.getPointsRequirementCondition().test(15)).isTrue();
        assertThat(UcPointsCondition.POINTS_LESS_THAN_FIFTEEN.getPointsRequirementCondition().test(15)).isFalse();
    }

    @Test
    public void testConditionWhenGreaterThan15() {
        assertThat(UcPointsCondition.ZERO_POINTS.pointsRequirementCondition.test(16)).isFalse();
        assertThat(UcPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN.getPointsRequirementCondition().test(16)).isTrue();
        assertThat(UcPointsCondition.POINTS_LESS_THAN_FIFTEEN.getPointsRequirementCondition().test(16)).isFalse();
    }

    /**
     * Test the continuity of boundaries between point ranges for daily living conditions. (ie. this test will fail if there are any gaps, or overlap between the boundaries)
     */
    @Test
    public void testThatAtExactlyOnePointsConditionPassesForPossiblePointValues() {

        int minPoints = 1;
        int maxPoints = 100;
        for (int points = minPoints; points < maxPoints; points++) {
            int pointsConditionSatisfiedCount = conditionsSatisfiedCountForPoints(points);

            if (pointsConditionSatisfiedCount != 1) {
                System.out.println("Expected 1 condition to be satisfied for points:" + points + " but " + pointsConditionSatisfiedCount + " were satisfied");
            }

            assertThat(pointsConditionSatisfiedCount).isEqualTo(1);
        }
    }

    // Zero points can occur when a case has not been awarded points for an activity, or when a case skips over the points-awarding question entirely (e.g. when the case has an SV issue code)
    @Test
    public void testZeroPointsCondition() {
        assertThat(conditionsSatisfiedCountForPoints(0)).isEqualTo(2);
    }

    @Test
    public void testAllPointsConditionAttributesAreNotNull() {
        for (UcPointsCondition ucPointsCondition : UcPointsCondition.values()) {
            assertThat(ucPointsCondition.getPointsRequirementCondition()).isNotNull();
        }
    }

    private int conditionsSatisfiedCountForPoints(int points) {
        int pointsConditionSatisfiedCount = 0;
        for (UcPointsCondition ucPointsCondition : UcPointsCondition.values()) {
            if (ucPointsCondition.getPointsRequirementCondition().test(points)) {
                pointsConditionSatisfiedCount++;
            }
        }
        return pointsConditionSatisfiedCount;
    }
}
