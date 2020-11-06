package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoPredicate.FALSE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoPredicate.SPECIFIED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoPredicate.TRUE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoPredicate.UNSPECIFIED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.FieldCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.PointsCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.StringListFieldCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.StringListPredicate;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoFieldCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoPredicate;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;
import uk.gov.hmcts.reform.sscs.utility.StringUtils;

/**
 * Encapsulates the conditions satisfied by valid journeys through the decision notice flow,
 * along with additional validation criteria and error message.  To be used as part of validation
 * before preview.
 */
public enum EsaPointsRegulationsAndSchedule3ActivitiesCondition implements PointsCondition<EsaPointsRegulationsAndSchedule3ActivitiesCondition> {

    LOW_POINTS_REGULATION_29_UNSPECIFIED(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        isRegulation29(UNSPECIFIED, false), Optional.empty(), isRegulation29(SPECIFIED)),
    LOW_POINTS_REGULATION_29_DOES_NOT_APPLY(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        isRegulation29(FALSE), Optional.of(AwardType.NO_AWARD), isRegulation35(UNSPECIFIED), isSchedule3ActivitiesAnswer(StringListPredicate.UNSPECIFIED)),
    LOW_POINTS_REGULATION_29_DOES_APPLY_REGULATION_35_UNSPECIFIED(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isRegulation29(TRUE), isRegulation35(UNSPECIFIED)), Optional.of(AwardType.HIGHER_RATE),  isSchedule3ActivitiesAnswer(StringListPredicate.NOT_EMPTY)),
    LOW_POINTS_REGULATION_29_DOES_APPLY_REGULATION_35_DOES_NOT_APPLY(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isRegulation29(TRUE), isRegulation35(FALSE)), Optional.of(AwardType.LOWER_RATE), isSchedule3ActivitiesAnswer(StringListPredicate.EMPTY)),
    LOW_POINTS_REGULATION_29_DOES_APPLY_REGULATION_35_DOES_APPLY(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isRegulation29(TRUE), isRegulation35(TRUE)), Optional.of(AwardType.HIGHER_RATE), isSchedule3ActivitiesAnswer(StringListPredicate.EMPTY)),
    HIGH_POINTS_REGULATION_35_UNSPECIFIED(EsaPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN,
        isRegulation35(UNSPECIFIED, false), Optional.of(AwardType.HIGHER_RATE), isRegulation29(UNSPECIFIED), isSchedule3ActivitiesAnswer(StringListPredicate.NOT_EMPTY)),
    HIGH_POINTS_REGULATION_35_DOES_NOT_APPLY(EsaPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN,
        isRegulation35(FALSE), Optional.of(AwardType.LOWER_RATE),  isRegulation29(UNSPECIFIED), isSchedule3ActivitiesAnswer(StringListPredicate.EMPTY)),
    HIGH_POINTS_REGULATION_35_DOES_APPLY(EsaPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN,
        isRegulation35(TRUE), Optional.of(AwardType.HIGHER_RATE),  isRegulation29(UNSPECIFIED), isSchedule3ActivitiesAnswer(StringListPredicate.EMPTY));

    List<YesNoFieldCondition> primaryConditions;
    List<FieldCondition> validationConditions;

    EsaPointsCondition pointsCondition;
    Optional<AwardType> awardType;

    EsaPointsRegulationsAndSchedule3ActivitiesCondition(EsaPointsCondition pointsCondition, YesNoFieldCondition primaryCondition,
        Optional<AwardType> awardType, FieldCondition...validationConditions) {
        this(pointsCondition, Arrays.asList(primaryCondition), awardType, validationConditions);
    }

    public Optional<AwardType> getAwardType() {
        return awardType;
    }

    EsaPointsRegulationsAndSchedule3ActivitiesCondition(EsaPointsCondition pointsCondition, List<YesNoFieldCondition> primaryConditions,
        Optional<AwardType> awardType, FieldCondition...validationConditions) {
        this.pointsCondition = pointsCondition;
        this.awardType = awardType;
        this.primaryConditions = primaryConditions;
        this.validationConditions = Arrays.asList(validationConditions);
    }

    static YesNoFieldCondition isRegulation29(YesNoPredicate predicate) {
        return new YesNoFieldCondition("Regulation 29", predicate,
                SscsCaseData::getDoesRegulation29Apply);
    }

    static YesNoFieldCondition isRegulation29(YesNoPredicate predicate, boolean displaySatisifiedMessageOnError) {
        return new YesNoFieldCondition("Regulation 29", predicate,
                SscsCaseData::getDoesRegulation29Apply, displaySatisifiedMessageOnError);
    }

    static YesNoFieldCondition isRegulation35(YesNoPredicate predicate) {
        return new YesNoFieldCondition("Regulation 35", predicate,
                SscsCaseData::getDoesRegulation35Apply);
    }

    static YesNoFieldCondition isRegulation35(YesNoPredicate predicate, boolean displaySatisifiedMessageOnError) {
        return new YesNoFieldCondition("Regulation 35", predicate,
                SscsCaseData::getDoesRegulation35Apply, displaySatisifiedMessageOnError);
    }

    static FieldCondition isSchedule3ActivitiesAnswer(StringListPredicate predicate) {
        return new StringListFieldCondition("Schedule 3 Activities", predicate,
                SscsCaseData::getEsaWriteFinalDecisionSchedule3ActivitiesQuestion);
    }

    @Override
    public boolean isApplicable(DecisionNoticeQuestionService questionService, SscsCaseData caseData) {
        int points = questionService.getTotalPoints(caseData, getAnswersExtractor().apply(caseData));
        return pointsCondition.getPointsRequirementCondition().test(points) && primaryConditions.stream().allMatch(c -> c.isSatisified(caseData));
    }

    @Override
    public IntPredicate getPointsRequirementCondition() {
        return pointsCondition.getPointsRequirementCondition();
    }

    @Override
    public Class<EsaPointsRegulationsAndSchedule3ActivitiesCondition> getEnumClass() {
        return EsaPointsRegulationsAndSchedule3ActivitiesCondition.class;
    }

    @Override
    public Function<SscsCaseData, List<String>> getAnswersExtractor() {
        return getAllAnswersExtractor();
    }

    public static EsaPointsRegulationsAndSchedule3ActivitiesCondition getTheSinglePassingPointsConditionForSubmittedActivitiesAndPoints(DecisionNoticeQuestionService questionService,
        SscsCaseData caseData) {
        for (EsaPointsRegulationsAndSchedule3ActivitiesCondition esaPointsAndActivitiesCondition : EsaPointsRegulationsAndSchedule3ActivitiesCondition.values()) {

            if (esaPointsAndActivitiesCondition.isApplicable(questionService, caseData) && esaPointsAndActivitiesCondition.getOptionalErrorMessage(questionService, caseData).isEmpty()) {
                return esaPointsAndActivitiesCondition;
            }
        }
        throw new IllegalStateException(
            "No points condition found for " + caseData.getDoesRegulation29Apply() + ":" + caseData.getEsaWriteFinalDecisionSchedule3ActivitiesQuestion() + ":" + caseData.getDoesRegulation35Apply());
    }

    public static Optional<EsaAllowedOrRefusedCondition> getPassingAllowedOrRefusedCondition(DecisionNoticeQuestionService questionService,
        SscsCaseData caseData) {

        EsaPointsRegulationsAndSchedule3ActivitiesCondition condition
            = getTheSinglePassingPointsConditionForSubmittedActivitiesAndPoints(questionService, caseData);

        if (condition.getOptionalErrorMessage(questionService, caseData).isEmpty()) {
            return Optional.of(EsaAllowedOrRefusedCondition.getTheSinglePassingPointsConditionForSubmittedActivitiesAndPoints(questionService, caseData));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> getOptionalErrorMessage(DecisionNoticeQuestionService questionService, SscsCaseData sscsCaseData) {

        List<String> primaryCriteriaSatisfiedMessages =
            primaryConditions.stream()
                .map(YesNoFieldCondition::getOptionalIsSatisfiedMessage)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        List<String> validationErrorMessages =
                validationConditions.stream()
                .map(e -> e.getOptionalErrorMessage(sscsCaseData))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        List<String> criteriaSatisfiedMessages = new ArrayList<>();
        criteriaSatisfiedMessages.add(pointsCondition.getIsSatisfiedMessage());
        criteriaSatisfiedMessages.addAll(primaryCriteriaSatisfiedMessages);

        if (!validationErrorMessages.isEmpty()) {
            return Optional.of("You have " + StringUtils.getGramaticallyJoinedStrings(criteriaSatisfiedMessages)
                    + (criteriaSatisfiedMessages.isEmpty() ? "" : ", but have ") + StringUtils.getGramaticallyJoinedStrings(validationErrorMessages)
               + ". Please review your previous selection.");
        }
        return Optional.empty();
    }

    public static Function<SscsCaseData, List<String>> getAllAnswersExtractor() {
        return sscsCaseData -> CollectionUtils.collate(emptyIfNull(sscsCaseData.getEsaWriteFinalDecisionPhysicalDisabilitiesQuestion()),
            emptyIfNull(sscsCaseData.getEsaWriteFinalDecisionMentalAssessmentQuestion()));
    }
}
