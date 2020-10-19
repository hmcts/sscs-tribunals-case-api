package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

/**
 * Enum encapsulating the attributes of a points-related condition on SscsCaseData. Each condition specifies the type of award the condition applies for, the activity type it applies to, along with
 * points criteria and an error message to display if the points criteria are not met.
 */
public enum EsaPointsAndActivitiesCondition {

    POINTS_LESS_THAN_FIFTEEN_AND_REGULATION_29_DOES_NOT_APPLY(
        EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        false,
        null, null,
        EsaAwardType.NO_AWARD),
    POINTS_LESS_THAN_FIFTEEN_AND_REGULATION_29_DOES_APPLY_AND_SCHEDULE_3_ACTIVITIES_SELECTED(
        EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        true,
        true, null,
        EsaAwardType.HIGHER_RATE),
    POINTS_LESS_THAN_FIFTEEN_AND_REGULATION_29_DOES_APPLY_AND_SCHEDULE_3_ACTIVITIES_NOT_SELECTED_AND_REGULATION_35_APPLIES(
        EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        true, false, true,
        EsaAwardType.HIGHER_RATE),
    POINTS_LESS_THAN_FIFTEEN_AND_REGULATION_29_DOES_APPLY_AND_SCHEDULE_3_ACTIVITIES_NOT_SELECTED_AND_REGULATION_35_DOES_NOT_APPLY(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        true, false, false,
        EsaAwardType.LOWER_RATE),
    POINTS_NOT_LESS_THAN_FIFTEEN_AND_SCHEDULE_3_ACTIVITIES_SELECTED(EsaPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN,
        null, true, null,
        EsaAwardType.HIGHER_RATE),
    POINTS_NOT_LESS_THAN_FIFTEEN_AND_SCHEDULE_3_ACTIVITIES_NOT_SELECTED_AND_REGULATION_35_APPLIES(EsaPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN,
        null, false, true,
        EsaAwardType.HIGHER_RATE),
    POINTS_NOT_LESS_THAN_FIFTEEN_AND_SCHEDULE_3_ACTIVITIES_NOT_SELECTED_AND_REGULATION_35_DOES_NOT_APPLY(EsaPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN,
        null, false, false,
        EsaAwardType.LOWER_RATE);

    final EsaAwardType awardType;
    final String errorMessage;
    final EsaPointsCondition esaPointsCondition;
    final Boolean regulation29Applies;
    final Boolean schedule3ActivitiesSelected;
    final Boolean doesRegulation35Apply;

    EsaPointsAndActivitiesCondition(EsaPointsCondition esaPointsCondition, Boolean regulation29Applies,
        Boolean schedule3ActivitesSelected,
        Boolean doesRegulation35Apply,
        EsaAwardType awardType) {
        this.awardType = awardType;
        this.esaPointsCondition = esaPointsCondition;
        this.regulation29Applies = regulation29Applies;
        this.schedule3ActivitiesSelected = schedule3ActivitesSelected;
        this.doesRegulation35Apply = doesRegulation35Apply;
        this.errorMessage = "";
    }

    public boolean isSatisified(int points, Boolean regulation29Applies, Boolean schedule3ActivitiesSelected, Boolean doesRegulation35Apply) {
        return esaPointsCondition.pointsRequirementCondition.test(points) && areEqual(this.regulation29Applies, regulation29Applies, "regulation29Applies")
            && areEqual(this.schedule3ActivitiesSelected, schedule3ActivitiesSelected, "schedule3ActiviteisSelected")
            && areEqual(this.doesRegulation35Apply, doesRegulation35Apply, "doesRegulation35Apply");
    }

    private boolean areEqual(Boolean first, Boolean second, String attributeName) {
        if (first == null) {
            if (second == null) {
                return true;
            } else {
                throw new IllegalStateException("No value expected for:" + attributeName + " but value was " + second);
            }
        } else {
            if (second != null) {
                return first.booleanValue() == second.booleanValue();
            } else {
                throw new IllegalStateException("Value expected for:" + attributeName + " but value was " + second);
            }
        }
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public EsaPointsCondition getPointsCondition() {
        return esaPointsCondition;
    }
}
