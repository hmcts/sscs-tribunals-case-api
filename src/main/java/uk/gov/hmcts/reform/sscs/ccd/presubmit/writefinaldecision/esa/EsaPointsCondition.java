package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import java.util.function.IntPredicate;

/**
 * Enum encapsulating the attributes of a points-related condition on SscsCaseData. Each condition specifies the points criteria.
 */
public enum EsaPointsCondition  {

    POINTS_LESS_THAN_FIFTEEN(points -> points < 15, "awarded less than 15 points"),
    POINTS_GREATER_OR_EQUAL_TO_FIFTEEN(points -> points >= 15, "awarded 15 points or more");

    final String isSatisfiedMessage;
    final IntPredicate pointsRequirementCondition;

    EsaPointsCondition(IntPredicate pointsRequirementCondition, String isSatisfiedMessage) {
        this.pointsRequirementCondition = pointsRequirementCondition;
        this.isSatisfiedMessage = isSatisfiedMessage;
    }

    public IntPredicate getPointsRequirementCondition() {
        return pointsRequirementCondition;
    }


    public String getIsSatisfiedMessage() {
        return isSatisfiedMessage;
    }

    public String getErrorMessage() {
        return "not " + getIsSatisfiedMessage();
    }


}
