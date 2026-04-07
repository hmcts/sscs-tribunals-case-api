package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class UcPointsConditionTest {

    @Test
    public void testConditionsWhenLessThan15() {
        assertThat(UcPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN.getPointsRequirementCondition().test(14)).isFalse();
        assertThat(UcPointsCondition.POINTS_LESS_THAN_FIFTEEN.getPointsRequirementCondition().test(14)).isTrue();
    }

    @Test
    public void testConditionWhenEqualTo15() {
        assertThat(UcPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN.getPointsRequirementCondition().test(15)).isTrue();
        assertThat(UcPointsCondition.POINTS_LESS_THAN_FIFTEEN.getPointsRequirementCondition().test(15)).isFalse();
    }

    @Test
    public void testConditionWhenGreaterThan15() {
        assertThat(UcPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN.getPointsRequirementCondition().test(16)).isTrue();
        assertThat(UcPointsCondition.POINTS_LESS_THAN_FIFTEEN.getPointsRequirementCondition().test(16)).isFalse();
    }

    /**
     * Test the continuity of boundaries between point ranges for daily living conditions. (ie. this test will fail if there are any gaps, or overlap between the boundaries)
     */
    @Test
    public void testThatAtExactlyOnePointsConditionPassesForPossiblePointValues() {

        int minPoints = 0;
        int maxPoints = 100;
        for (int points = minPoints; points < maxPoints; points++) {

            int pointsConditionSatisfiedCount = 0;
            for (UcPointsCondition ucPointsCondition : UcPointsCondition.values()) {
                if (ucPointsCondition.getPointsRequirementCondition().test(points)) {
                    pointsConditionSatisfiedCount++;
                }
            }

            if (pointsConditionSatisfiedCount != 1) {
                System.out.println("Expected 1 condition to be satisfied for points:" + points + " but " + pointsConditionSatisfiedCount + " were satisfied");
            }

            assertThat(pointsConditionSatisfiedCount).isEqualTo(1);
        }
    }

    @Test
    public void testAllPointsConditionAttributesAreNotNull() {
        for (UcPointsCondition ucPointsCondition : UcPointsCondition.values()) {
            assertThat(ucPointsCondition.getPointsRequirementCondition()).isNotNull();
        }
    }
}
