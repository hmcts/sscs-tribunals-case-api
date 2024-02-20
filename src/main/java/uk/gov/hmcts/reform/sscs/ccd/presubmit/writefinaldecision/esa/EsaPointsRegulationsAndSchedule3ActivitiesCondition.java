package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoPredicate.FALSE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoPredicate.NOT_TRUE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoPredicate.SPECIFIED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoPredicate.TRUE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoPredicate.UNSPECIFIED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.collections4.CollectionUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
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

    // Used for the scenario where case points start high but are lowered and regulation 29 question is skipped
    LOW_POINTS_REGULATION_29_UNSPECIFIED(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isWcaAppeal(TRUE), isSupportGroupOnly(NOT_TRUE, true), isRegulation29(UNSPECIFIED, false)), Optional.empty(), true, isRegulation29(SPECIFIED)),
    // Scenario 1
    LOW_POINTS_REGULATION_29_DOES_NOT_APPLY(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isWcaAppeal(TRUE), isSupportGroupOnly(NOT_TRUE, true), isRegulation29(FALSE)), Optional.of(AwardType.NO_AWARD), true, isRegulation35(UNSPECIFIED), isSchedule3ActivitiesAnswer(StringListPredicate.UNSPECIFIED)),
    // SCENARIO_9
    LOW_POINTS_REGULATION_29_DOES_APPLY_REGULATION_35_UNSPECIFIED_NON_SUPPORT_GROUP_ONLY(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isWcaAppeal(TRUE), isSupportGroupOnly(NOT_TRUE, true), isRegulation29(TRUE), isRegulation35(UNSPECIFIED)), Optional.of(AwardType.HIGHER_RATE),  true, isSchedule3ActivitiesAnswer(StringListPredicate.NOT_EMPTY)),
    // SCENARIO_7
    LOW_POINTS_REGULATION_29_DOES_APPLY_REGULATION_35_DOES_NOT_APPLY_NON_SUPPORT_GROUP_ONLY(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isWcaAppeal(TRUE), isSupportGroupOnly(NOT_TRUE, true), isRegulation29(TRUE), isRegulation35(FALSE)), Optional.of(AwardType.LOWER_RATE), true, isSchedule3ActivitiesAnswer(StringListPredicate.EMPTY)),
    // SCENARIO_8
    LOW_POINTS_REGULATION_29_DOES_APPLY_REGULATION_35_DOES_APPLY_NON_SUPPORT_GROUP_ONLY(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isWcaAppeal(TRUE), isSupportGroupOnly(NOT_TRUE, true), isRegulation29(TRUE), isRegulation35(TRUE)), Optional.of(AwardType.HIGHER_RATE), true, isSchedule3ActivitiesAnswer(StringListPredicate.EMPTY)),
    // SCENARIO_4
    LOW_POINTS_SCHEDULE2_AND_REG_29_SKIPPED_REGULATION_35_UNSPECIFIED_SUPPORT_GROUP_ONLY(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isWcaAppeal(TRUE), isSupportGroupOnly(TRUE, true), isRegulation35(UNSPECIFIED)), Optional.of(AwardType.HIGHER_RATE),  false, isSchedule3ActivitiesAnswer(StringListPredicate.NOT_EMPTY), isRegulation29(UNSPECIFIED)),
    // SCENARIO_2
    LOW_POINTS_SCHEDULE2_AND_REG_29_SKIPPED_REGULATION_35_DOES_NOT_APPLY_SUPPORT_GROUP_ONLY(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isWcaAppeal(TRUE), isSupportGroupOnly(TRUE, true), isRegulation35(FALSE)), Optional.of(AwardType.LOWER_RATE), false, isSchedule3ActivitiesAnswer(StringListPredicate.EMPTY), isRegulation29(UNSPECIFIED)),
    // SCENARIO_3
    LOW_POINTS_SCHEDULE2_AND_REG_29_SKIPPED_REGULATION_35_DOES_APPLY_SUPPORT_GROUP_ONLY(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isWcaAppeal(TRUE), isSupportGroupOnly(TRUE, true),  isRegulation35(TRUE)), Optional.of(AwardType.HIGHER_RATE), false, isSchedule3ActivitiesAnswer(StringListPredicate.EMPTY), isRegulation29(UNSPECIFIED)),
    // SCENARIO_6
    HIGH_POINTS_REGULATION_35_UNSPECIFIED(EsaPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN,
        isWcaAppeal(TRUE), isRegulation35(UNSPECIFIED), Optional.of(AwardType.HIGHER_RATE), true, isRegulation29(UNSPECIFIED), isSupportGroupOnly(NOT_TRUE, true), isSchedule3ActivitiesAnswer(StringListPredicate.NOT_EMPTY)),
    // SCENARIO_5
    HIGH_POINTS_REGULATION_35_DOES_NOT_APPLY(EsaPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN,
        isWcaAppeal(TRUE),isRegulation35(FALSE), Optional.of(AwardType.LOWER_RATE),  true, isRegulation29(UNSPECIFIED), isSupportGroupOnly(NOT_TRUE, true), isSchedule3ActivitiesAnswer(StringListPredicate.EMPTY)),
    // SCENARIO_5
    HIGH_POINTS_REGULATION_35_DOES_APPLY(EsaPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN,
        isWcaAppeal(TRUE), isRegulation35(TRUE), Optional.of(AwardType.HIGHER_RATE),  true, isRegulation29(UNSPECIFIED), isSupportGroupOnly(NOT_TRUE, true), isSchedule3ActivitiesAnswer(StringListPredicate.EMPTY)),
    // SCENARIO 10
    NON_WCA_APPEAL(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
            Arrays.asList(isWcaAppeal(FALSE)), Optional.empty(), false, isDwpReassessTheAward(UNSPECIFIED));
    List<YesNoFieldCondition> primaryConditions;
    List<FieldCondition> validationConditions;

    EsaPointsCondition pointsCondition;
    Optional<AwardType> awardType;
    private boolean displayPointsSatisfiedMessageOnError;

    EsaPointsRegulationsAndSchedule3ActivitiesCondition(EsaPointsCondition pointsCondition, YesNoFieldCondition primaryCondition1,YesNoFieldCondition primaryCondition2,
        Optional<AwardType> awardType, boolean displayPointsSatisfiedMessageOnError, FieldCondition...validationConditions) {
        this(pointsCondition, Arrays.asList(primaryCondition1, primaryCondition2), awardType, displayPointsSatisfiedMessageOnError, validationConditions);
        this.displayPointsSatisfiedMessageOnError = displayPointsSatisfiedMessageOnError;
    }

    public Optional<AwardType> getAwardType() {
        return awardType;
    }

    EsaPointsRegulationsAndSchedule3ActivitiesCondition(EsaPointsCondition pointsCondition, List<YesNoFieldCondition> primaryConditions,
        Optional<AwardType> awardType, boolean displayPointsSatisfiedMessageOnError, FieldCondition...validationConditions) {
        this.pointsCondition = pointsCondition;
        this.awardType = awardType;
        this.primaryConditions = primaryConditions;
        this.validationConditions = Arrays.asList(validationConditions);
        this.displayPointsSatisfiedMessageOnError = displayPointsSatisfiedMessageOnError;
    }

    static YesNoFieldCondition isRegulation29(YesNoPredicate predicate) {
        return new YesNoFieldCondition("Regulation 29", predicate,
            caseData -> caseData.getSscsEsaCaseData().getDoesRegulation29Apply());
    }

    static YesNoFieldCondition isRegulation29(YesNoPredicate predicate, boolean displaySatisifiedMessageOnError) {
        return new YesNoFieldCondition("Regulation 29", predicate,
            caseData -> caseData.getSscsEsaCaseData().getDoesRegulation29Apply(), displaySatisifiedMessageOnError);
    }

    static YesNoFieldCondition isSupportGroupOnly(Predicate<YesNo> predicate, boolean displayPointsSatisfiedMessageOnError) {
        return new YesNoFieldCondition("Support Group Only Appeal", predicate,
            s -> s.getSupportGroupOnlyAppeal() == null ? null : s.isSupportGroupOnlyAppeal() ? YesNo.YES : YesNo.NO, displayPointsSatisfiedMessageOnError);
    }

    static YesNoFieldCondition isRegulation35(Predicate<YesNo> predicate) {
        return new YesNoFieldCondition("Regulation 35", predicate,
            caseData -> caseData.getSscsEsaCaseData().getRegulation35Selection());
    }

    static YesNoFieldCondition isRegulation35(YesNoPredicate predicate, boolean displaySatisifiedMessageOnError) {
        return new YesNoFieldCondition("Regulation 35", predicate,
            caseData -> caseData.getSscsEsaCaseData().getRegulation35Selection(), displaySatisifiedMessageOnError);
    }

    static YesNoFieldCondition isWcaAppeal(Predicate<YesNo> predicate) {
        return new YesNoFieldCondition("Wca Appeal", predicate,
            sscsCaseData -> sscsCaseData.isWcaAppeal() ? YesNo.YES : YesNo.NO, false);
    }

    static YesNoFieldCondition isDwpReassessTheAward(Predicate<YesNo> predicate) {
        return new YesNoFieldCondition("'When should DWP reassess the award?'", predicate,
            (SscsCaseData sscsCaseData) -> sscsCaseData.getDwpReassessTheAward() == null ? null : isNotBlank(sscsCaseData.getDwpReassessTheAward()) ? YesNo.YES : YesNo.NO);
    }

    static FieldCondition isSchedule3ActivitiesAnswer(StringListPredicate predicate) {
        return new StringListFieldCondition("Schedule 3 Activities", predicate,
            caseData -> caseData.getSscsEsaCaseData().getSchedule3Selections());
    }

    @Override
    public boolean isApplicable(DecisionNoticeQuestionService questionService, SscsCaseData caseData) {
        int points = questionService.getTotalPoints(caseData, getAnswersExtractor().apply(caseData));
        return isYes(caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGenerateNotice()) && pointsCondition.getPointsRequirementCondition().test(points) && primaryConditions.stream().allMatch(c -> c.isSatisified(caseData));
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
            "No points condition found for " + caseData.getSscsEsaCaseData().getDoesRegulation29Apply() + ":" + caseData.getSscsEsaCaseData().getSchedule3Selections() + ":" + caseData.getSscsEsaCaseData().getRegulation35Selection());
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
                .map(c -> c.getOptionalIsSatisfiedMessage(sscsCaseData))
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
        if (displayPointsSatisfiedMessageOnError) {
            criteriaSatisfiedMessages.add(pointsCondition.getIsSatisfiedMessage());
        }
        criteriaSatisfiedMessages.addAll(primaryCriteriaSatisfiedMessages);

        if (!validationErrorMessages.isEmpty()) {
            return Optional.of("You have " + StringUtils.getGramaticallyJoinedStrings(criteriaSatisfiedMessages)
                    + (criteriaSatisfiedMessages.isEmpty() ? "" : ", but have ") + StringUtils.getGramaticallyJoinedStrings(validationErrorMessages)
               + ". Please review your previous selection.");
        }
        return Optional.empty();
    }

    public static Function<SscsCaseData, List<String>> getAllAnswersExtractor() {
        return sscsCaseData -> CollectionUtils.collate(emptyIfNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionPhysicalDisabilitiesQuestion()),
            emptyIfNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionMentalAssessmentQuestion()));
    }
}
