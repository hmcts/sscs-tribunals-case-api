package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType.HIGHER_RATE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType.LOWER_RATE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType.NO_AWARD;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntPredicate;
import org.apache.commons.collections4.CollectionUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.PointsCondition;

/**
 * Enum encapsulating the attributes of a points-related condition on SscsCaseData. Each condition specifies the type of award the condition applies for, the activity type it applies to, along with
 * points criteria and an error message to display if the points criteria are not met.
 */
public enum EsaPointsAndActivitiesCondition implements PointsCondition<EsaPointsAndActivitiesCondition> {

    POINTS_LESS_THAN_FIFTEEN_AND_REGULATION_29_DOES_NOT_APPLY(
        POINTS_LESS_THAN_FIFTEEN,
        false,
        null, null,
        NO_AWARD),
    POINTS_LESS_THAN_FIFTEEN_AND_REGULATION_29_DOES_APPLY_AND_SCHEDULE_3_ACTIVITIES_SELECTED(
        POINTS_LESS_THAN_FIFTEEN,
        true,
        true, null,
        HIGHER_RATE),
    POINTS_LESS_THAN_FIFTEEN_AND_REGULATION_29_DOES_APPLY_AND_SCHEDULE_3_ACTIVITIES_NOT_SELECTED_AND_REGULATION_35_APPLIES(
        POINTS_LESS_THAN_FIFTEEN,
        true, false, true,
        HIGHER_RATE),
    POINTS_LESS_THAN_FIFTEEN_AND_REGULATION_29_DOES_APPLY_AND_SCHEDULE_3_ACTIVITIES_NOT_SELECTED_AND_REGULATION_35_DOES_NOT_APPLY(POINTS_LESS_THAN_FIFTEEN,
        true, false, false,
        LOWER_RATE),
    POINTS_NOT_LESS_THAN_FIFTEEN_AND_SCHEDULE_3_ACTIVITIES_SELECTED(POINTS_GREATER_OR_EQUAL_TO_FIFTEEN,
        null, true, null,
        HIGHER_RATE),
    POINTS_NOT_LESS_THAN_FIFTEEN_AND_SCHEDULE_3_ACTIVITIES_NOT_SELECTED_AND_REGULATION_35_APPLIES(POINTS_GREATER_OR_EQUAL_TO_FIFTEEN,
        null, false, true,
        HIGHER_RATE),
    POINTS_NOT_LESS_THAN_FIFTEEN_AND_SCHEDULE_3_ACTIVITIES_NOT_SELECTED_AND_REGULATION_35_DOES_NOT_APPLY(POINTS_GREATER_OR_EQUAL_TO_FIFTEEN,
        null, false, false,
        LOWER_RATE);

    final AwardType awardType;
    final String errorMessage;
    final EsaPointsCondition esaPointsCondition;
    final Boolean regulation29Applies;
    final Boolean schedule3ActivitiesSelected;
    final Boolean doesRegulation35Apply;

    EsaPointsAndActivitiesCondition(EsaPointsCondition esaPointsCondition, Boolean regulation29Applies,
        Boolean schedule3ActivitesSelected,
        Boolean doesRegulation35Apply,
        AwardType awardType) {
        this.awardType = awardType;
        this.esaPointsCondition = esaPointsCondition;
        this.regulation29Applies = regulation29Applies;
        this.schedule3ActivitiesSelected = schedule3ActivitesSelected;
        this.doesRegulation35Apply = doesRegulation35Apply;
        this.errorMessage = "";
    }

    public boolean isRegulation29QuestionRequired() {
        return regulation29Applies != null;
    }

    public boolean isRegulation35QuestionRequired() {
        return doesRegulation35Apply != null;
    }

    public static Optional<EsaPointsAndActivitiesCondition> getPointsAndActivitiesCondition(SscsCaseData caseData, int points) {
        for (EsaPointsAndActivitiesCondition condition : EsaPointsAndActivitiesCondition.values()) {
            if (condition.isSatisified(points,
                caseData.getDoesRegulation29Apply() == null ? null : Boolean.valueOf(YesNo.YES.equals(caseData.getDoesRegulation29Apply())),
                getSchedule3ActivitiesSelected(caseData),
                caseData.getDoesRegulation35Apply() == null ? null : Boolean.valueOf(YesNo.YES.equals(caseData.getDoesRegulation35Apply())))) {
                return Optional.of(condition);
            }
        }
        return Optional.empty();
    }

    private static Boolean getSchedule3ActivitiesSelected(SscsCaseData caseData) {
        return false;
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

    @Override
    public boolean isApplicable(SscsCaseData caseData) {
        return true;
    }

    @Override
    public IntPredicate getPointsRequirementCondition() {
        return esaPointsCondition.getPointsRequirementCondition();
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public Class<EsaPointsAndActivitiesCondition> getEnumClass() {
        return EsaPointsAndActivitiesCondition.class;
    }

    @Override
    public Function<SscsCaseData, List<String>> getAnswersExtractor() {
        return sscsCaseData -> CollectionUtils.collate(sscsCaseData.getEsaWriteFinalDecisionPhysicalDisabilitiesQuestion(),
            sscsCaseData.getEsaWriteFinalDecisionMentalAssessmentQuestion());
    }

    public EsaPointsCondition getPointsCondition() {
        return esaPointsCondition;
    }
}
