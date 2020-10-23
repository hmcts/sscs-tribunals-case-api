package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType.HIGHER_RATE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType.LOWER_RATE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType.NO_AWARD;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntPredicate;
import org.apache.commons.collections4.CollectionUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.PointsCondition;
import uk.gov.hmcts.reform.sscs.utility.StringUtils;

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
        this.errorMessage = getStandardErrorMessage(regulation29Applies, schedule3ActivitiesSelected, doesRegulation35Apply);
    }

    public boolean isRegulation29QuestionRequired() {
        return regulation29Applies != null;
    }

    public boolean isRegulation35QuestionRequired() {
        return doesRegulation35Apply != null;
    }

    public static Optional<EsaPointsAndActivitiesCondition> getSatisifiedPointsCondition(SscsCaseData caseData, int points) {
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
        return null;
    }

    public boolean isSatisified(int points, Boolean regulation29Applies, Boolean schedule3ActivitiesSelected, Boolean doesRegulation35Apply) {
        return esaPointsCondition.pointsRequirementCondition.test(points) && areEqual(this.regulation29Applies, regulation29Applies, "regulation29Applies", true)
            && areEqual(this.schedule3ActivitiesSelected, schedule3ActivitiesSelected, "schedule3ActiviteisSelected", true)
            && areEqual(this.doesRegulation35Apply, doesRegulation35Apply, "doesRegulation35Apply", true);
    }

    private boolean areEqual(Boolean first, Boolean second, String attributeName, boolean throwExceptionIfIllegalState) {
        if (first == null) {
            if (second == null) {
                return true;
            } else {
                if (throwExceptionIfIllegalState) {
                    throw new IllegalStateException("No value expected for:" + attributeName + " but value was " + second);
                } else {
                    return false;
                }
            }
        } else {
            if (second != null) {
                return first.booleanValue() == second.booleanValue();
            } else {
                if (throwExceptionIfIllegalState) {
                    throw new IllegalStateException("Value expected for:" + attributeName + " but value was " + second);
                } else {
                    return false;
                }
            }
        }
    }

    protected static String getStandardErrorMessage(Boolean regulation29Applies, Boolean schedule3ActivitiesSelected, Boolean doesRegulation35Apply) {
        List<String> states = new ArrayList<>();
        if (regulation29Applies == null) {
            states.add("a missing answer for the Regulation 29 question");
        } else {
            states.add(regulation29Applies.booleanValue() ? "that Regulation 29 applies" : "that Regulation 29 does not apply");
        }
        if (doesRegulation35Apply == null) {
            states.add("a missing answer for the Regulation 35 question");
        } else {
            states.add(doesRegulation35Apply.booleanValue() ? "that Regulation 35 applies" : "that Regulation 35 does not apply");
        }
        if (schedule3ActivitiesSelected == null || !schedule3ActivitiesSelected.booleanValue()) {
            states.add("unexpected answers for the Schedule 3 activities");
        } else {
            states.add("a number of selections for the Schedule 3 activities");
        }

        return "You have submitted " + StringUtils.getGramaticallyJoinedStrings(states)
            + ". The points awarded don't match. Please review your previous selection.";
    }

    @Override
    public boolean isApplicable(SscsCaseData caseData) {

        return areEqual(this.regulation29Applies,
            caseData.getDoesRegulation29Apply() == null ? null : caseData.getDoesRegulation29Apply().toBoolean(), "regulation29", false) && areEqual(this.doesRegulation35Apply,
            caseData.getDoesRegulation35Apply() == null ? null : caseData.getDoesRegulation35Apply().toBoolean(), "regulation35", false);

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


    public static Function<SscsCaseData, List<String>> getCommonAnswersExtractor() {
        return sscsCaseData -> CollectionUtils.collate(emptyIfNull(sscsCaseData.getEsaWriteFinalDecisionPhysicalDisabilitiesQuestion()),
            emptyIfNull(sscsCaseData.getEsaWriteFinalDecisionMentalAssessmentQuestion()));
    }

    @Override
    public Function<SscsCaseData, List<String>> getAnswersExtractor() {
        return getCommonAnswersExtractor();
    }

    public EsaPointsCondition getPointsCondition() {
        return esaPointsCondition;
    }
}
