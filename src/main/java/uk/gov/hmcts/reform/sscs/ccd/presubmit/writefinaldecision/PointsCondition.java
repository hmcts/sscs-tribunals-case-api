package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import java.util.function.IntPredicate;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

/**
 * Enum encapsulating the attributes of a points-related condition on SscsCaseData.
 * Each condition specifies the type of award the condition applies for, the activity type
 * it applies to, along with points criteria and an error message to display if the points criteria
 * are not met.
 */
public enum PointsCondition {

  DAILY_LIVING_STANDARD(AwardType.STANDARD_RATE,
      ActivityType.DAILY_LIVING,
      points -> points >=8 && points <= 11,
      "You have previously selected a standard rate award for Daily Living. "
          + "The points awarded don't match. Please review your previous selection."),
  DAILY_LIVING_ENHANCED(AwardType.ENHANCED_RATE,
      ActivityType.DAILY_LIVING,
      points -> points >= 12,
      "You have previously selected an enhanced rate award for Daily Living. "
          + "The points awarded don't match. Please review your previous selection."),
  DAILY_LIVING_NO_AWARD(AwardType.NO_AWARD,
      ActivityType.DAILY_LIVING,
      points -> points <= 7,
      "You have previously selected No Award for Daily Living. "
          + "The points awarded don't match. Please review your previous selection."),
  MOBILITY_STANDARD(AwardType.STANDARD_RATE,
      ActivityType.MOBILITY,
      points -> points >=8 && points <= 11,
      "You have previously selected a standard rate award for Mobility. "
          + "The points awarded don't match. Please review your previous selection."),
  MOBILITY_ENHANCED(AwardType.ENHANCED_RATE,
      ActivityType.MOBILITY,
      points -> points >= 12,
      "You have previously selected an enhanced rate award for Mobility. "
          + "The points awarded don't match. Please review your previous selection."),
  MOBILITY_NO_AWARD(AwardType.NO_AWARD,
      ActivityType.MOBILITY,
      points -> points <= 7,
      "You have previously selected No Award for Mobility. "
          + "The points awarded don't match. Please review your previous selection.");

  final AwardType awardType;
  final String errorMessage;
  final ActivityType activityType;
  final IntPredicate pointsRequirementCondition;

  PointsCondition(AwardType awardType, ActivityType activityType,
      IntPredicate pointsRequirementCondition, String errorMessage) {
    this.awardType = awardType;
    this.pointsRequirementCondition = pointsRequirementCondition;
    this.activityType = activityType;
    this.errorMessage = errorMessage;
  }

  public boolean isApplicable(SscsCaseData sscsCaseData) {
    // Extract the relevant award rate from the sscsCaseData and compare with the key specified
    // for this condition.
    return awardType.getKey().equals(activityType.getAwardTypeExtractor().apply(sscsCaseData));
  }

  public ActivityType getActivityType() {
    return activityType;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

  public IntPredicate getPointsRequirementCondition() {
    return pointsRequirementCondition;
  }
}
