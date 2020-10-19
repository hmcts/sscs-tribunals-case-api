package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import java.util.function.IntPredicate;

/**
 * Enum encapsulating the attributes of a points-related condition on SscsCaseData. Each condition specifies the points criteria.
 */
public enum EsaPointsCondition {

    POINTS_LESS_THAN_FIFTEEN(points -> points < 15),
    POINTS_GREATER_OR_EQUAL_TO_FIFTEEN(points -> points >= 15);

    final IntPredicate pointsRequirementCondition;

    EsaPointsCondition(IntPredicate pointsRequirementCondition) {
        this.pointsRequirementCondition = pointsRequirementCondition;
    }

    public IntPredicate getPointsRequirementCondition() {
        return pointsRequirementCondition;
    }
}
