package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

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
public enum UcPointsRegulationsAndSchedule7ActivitiesCondition implements PointsCondition<UcPointsRegulationsAndSchedule7ActivitiesCondition> {

    // Used for the scenario where case points start high but are lowered and regulation 29 question is skipped
    LOW_POINTS_SCHEDULE_8_PARAGRAPH_4_UNSPECIFIED(UcPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isWcaAppeal(TRUE), isSupportGroupOnly(NOT_TRUE, true), isSchedule8Paragraph4(UNSPECIFIED, false)), Optional.empty(), true, isSchedule8Paragraph4(SPECIFIED)),
    // Scenario 1
    LOW_POINTS_SCHEDULE_8_PARAGRAPH_4_DOES_NOT_APPLY(UcPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isWcaAppeal(TRUE), isSupportGroupOnly(NOT_TRUE, true), isSchedule8Paragraph4(FALSE)), Optional.of(AwardType.NO_AWARD), true, isSchedule9Paragraph4(UNSPECIFIED), isSchedule7ActivitiesAnswer(
        StringListPredicate.UNSPECIFIED)),
    // SCENARIO_9
    LOW_POINTS_SCHEDULE_8_PARAGRAPH_4_DOES_APPLY_SCHEDULE_9_PARAGRAPH_4_UNSPECIFIED_NON_SUPPORT_GROUP_ONLY(UcPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isWcaAppeal(TRUE), isSupportGroupOnly(NOT_TRUE, true), isSchedule8Paragraph4(TRUE), isSchedule9Paragraph4(UNSPECIFIED)), Optional.of(AwardType.HIGHER_RATE),  true, isSchedule7ActivitiesAnswer(
        StringListPredicate.NOT_EMPTY)),
    // SCENARIO_7
    LOW_POINTS_SCHEDULE_8_PARAGRAPH_4_DOES_APPLY_SCHEDULE_9_PARAGRAPH_4_DOES_NOT_APPLY_NON_SUPPORT_GROUP_ONLY(UcPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isWcaAppeal(TRUE), isSupportGroupOnly(NOT_TRUE, true), isSchedule8Paragraph4(TRUE), isSchedule9Paragraph4(FALSE)), Optional.of(AwardType.LOWER_RATE), true, isSchedule7ActivitiesAnswer(
        StringListPredicate.EMPTY)),
    // SCENARIO_8
    LOW_POINTS_SCHEDULE_8_PARAGRAPH_4_DOES_APPLY_SCHEDULE_9_PARAGRAPH_4_DOES_APPLY_NON_SUPPORT_GROUP_ONLY(UcPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isWcaAppeal(TRUE), isSupportGroupOnly(NOT_TRUE, true), isSchedule8Paragraph4(TRUE), isSchedule9Paragraph4(TRUE)), Optional.of(AwardType.HIGHER_RATE), true, isSchedule7ActivitiesAnswer(
        StringListPredicate.EMPTY)),
    // SCENARIO_4
    LOW_POINTS_SCHEDULE6_AND_REG_29_SKIPPED_SCHEDULE_9_PARAGRAPH_4_UNSPECIFIED_SUPPORT_GROUP_ONLY(UcPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isWcaAppeal(TRUE), isSupportGroupOnly(TRUE, true), isSchedule9Paragraph4(UNSPECIFIED)), Optional.of(AwardType.HIGHER_RATE),  false, isSchedule7ActivitiesAnswer(
        StringListPredicate.NOT_EMPTY), isSchedule8Paragraph4(UNSPECIFIED)),
    // SCENARIO_2
    LOW_POINTS_SCHEDULE6_AND_REG_29_SKIPPED_SCHEDULE_9_PARAGRAPH_4_DOES_NOT_APPLY_SUPPORT_GROUP_ONLY(UcPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isWcaAppeal(TRUE), isSupportGroupOnly(TRUE, true), isSchedule9Paragraph4(FALSE)), Optional.of(AwardType.LOWER_RATE), false, isSchedule7ActivitiesAnswer(StringListPredicate.EMPTY), isSchedule8Paragraph4(UNSPECIFIED)),
    // SCENARIO_3
    LOW_POINTS_SCHEDULE6_AND_REG_29_SKIPPED_SCHEDULE_9_PARAGRAPH_4_DOES_APPLY_SUPPORT_GROUP_ONLY(UcPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isWcaAppeal(TRUE), isSupportGroupOnly(TRUE, true),  isSchedule9Paragraph4(TRUE)), Optional.of(AwardType.HIGHER_RATE), false, isSchedule7ActivitiesAnswer(StringListPredicate.EMPTY), isSchedule8Paragraph4(UNSPECIFIED)),
    // SCENARIO_6
    HIGH_POINTS_SCHEDULE_9_PARAGRAPH_4_UNSPECIFIED(UcPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN,
        isWcaAppeal(TRUE), isSchedule9Paragraph4(UNSPECIFIED), Optional.of(AwardType.HIGHER_RATE), true, isSchedule8Paragraph4(UNSPECIFIED), isSupportGroupOnly(NOT_TRUE, true), isSchedule7ActivitiesAnswer(
        StringListPredicate.NOT_EMPTY)),
    // SCENARIO_5
    HIGH_POINTS_SCHEDULE_9_PARAGRAPH_4_DOES_NOT_APPLY(UcPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN,
        isWcaAppeal(TRUE), isSchedule9Paragraph4(FALSE), Optional.of(AwardType.LOWER_RATE),  true, isSchedule8Paragraph4(UNSPECIFIED), isSupportGroupOnly(NOT_TRUE, true), isSchedule7ActivitiesAnswer(
        StringListPredicate.EMPTY)),
    // SCENARIO_5
    HIGH_POINTS_SCHEDULE_9_PARAGRAPH_4_DOES_APPLY(UcPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN,
        isWcaAppeal(TRUE), isSchedule9Paragraph4(TRUE), Optional.of(AwardType.HIGHER_RATE),  true, isSchedule8Paragraph4(UNSPECIFIED), isSupportGroupOnly(NOT_TRUE, true), isSchedule7ActivitiesAnswer(
        StringListPredicate.EMPTY)),
    // SCENARIO 10
    NON_WCA_APPEAL(UcPointsCondition.POINTS_LESS_THAN_FIFTEEN,
            Arrays.asList(isWcaAppeal(FALSE)), Optional.empty(), false, isDwpReassessTheAward(UNSPECIFIED));
    List<YesNoFieldCondition> primaryConditions;
    List<FieldCondition> validationConditions;

    UcPointsCondition pointsCondition;
    Optional<AwardType> awardType;
    private boolean displayPointsSatisfiedMessageOnError;

    UcPointsRegulationsAndSchedule7ActivitiesCondition(UcPointsCondition pointsCondition, YesNoFieldCondition primaryCondition1, YesNoFieldCondition primaryCondition2,
        Optional<AwardType> awardType, boolean displayPointsSatisfiedMessageOnError, FieldCondition...validationConditions) {
        this(pointsCondition, Arrays.asList(primaryCondition1, primaryCondition2), awardType, displayPointsSatisfiedMessageOnError, validationConditions);
        this.displayPointsSatisfiedMessageOnError = displayPointsSatisfiedMessageOnError;
    }

    public Optional<AwardType> getAwardType() {
        return awardType;
    }

    UcPointsRegulationsAndSchedule7ActivitiesCondition(UcPointsCondition pointsCondition, List<YesNoFieldCondition> primaryConditions,
        Optional<AwardType> awardType, boolean displayPointsSatisfiedMessageOnError, FieldCondition...validationConditions) {
        this.pointsCondition = pointsCondition;
        this.awardType = awardType;
        this.primaryConditions = primaryConditions;
        this.validationConditions = Arrays.asList(validationConditions);
        this.displayPointsSatisfiedMessageOnError = displayPointsSatisfiedMessageOnError;
    }

    static YesNoFieldCondition isSchedule8Paragraph4(YesNoPredicate predicate) {
        return new YesNoFieldCondition("Schedule 8 Paragraph 4", predicate,
            caseData -> caseData.getSscsUcCaseData().getDoesSchedule8Paragraph4Apply());
    }

    static YesNoFieldCondition isSchedule8Paragraph4(YesNoPredicate predicate, boolean displaySatisifiedMessageOnError) {
        return new YesNoFieldCondition("Schedule 8 Paragraph 4", predicate,
            caseData -> caseData.getSscsUcCaseData().getDoesSchedule8Paragraph4Apply(), displaySatisifiedMessageOnError);
    }

    static YesNoFieldCondition isSupportGroupOnly(Predicate<YesNo> predicate, boolean displayPointsSatisfiedMessageOnError) {
        return new YesNoFieldCondition("Support Group Only Appeal", predicate,
            s -> s.getSupportGroupOnlyAppeal() == null ? null : s.isSupportGroupOnlyAppeal() ? YesNo.YES : YesNo.NO, displayPointsSatisfiedMessageOnError);
    }

    static YesNoFieldCondition isSchedule9Paragraph4(Predicate<YesNo> predicate) {
        return new YesNoFieldCondition("Schedule 9 Paragraph 4", predicate,
            caseData -> caseData.getSscsUcCaseData().getSchedule9Paragraph4Selection());
    }

    static YesNoFieldCondition isSchedule9Paragraph4(YesNoPredicate predicate, boolean displaySatisifiedMessageOnError) {
        return new YesNoFieldCondition("Schedule 9 Paragraph 4", predicate,
            caseData -> caseData.getSscsUcCaseData().getSchedule9Paragraph4Selection(), displaySatisifiedMessageOnError);
    }

    static YesNoFieldCondition isWcaAppeal(Predicate<YesNo> predicate) {
        return new YesNoFieldCondition("WCA Appeal", predicate,
            sscsCaseData -> sscsCaseData.isWcaAppeal() ? YesNo.YES : YesNo.NO, false);
    }

    static YesNoFieldCondition isDwpReassessTheAward(Predicate<YesNo> predicate) {
        return new YesNoFieldCondition("'When should DWP reassess the award?'", predicate,
            (SscsCaseData sscsCaseData) -> sscsCaseData.getDwpReassessTheAward() == null ? null : isNotBlank(sscsCaseData.getDwpReassessTheAward()) ? YesNo.YES : YesNo.NO);
    }

    static FieldCondition isSchedule7ActivitiesAnswer(StringListPredicate predicate) {
        return new StringListFieldCondition("Schedule 7 Activities", predicate,
            caseData -> caseData.getSscsUcCaseData().getSchedule7Selections());
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
    public Class<UcPointsRegulationsAndSchedule7ActivitiesCondition> getEnumClass() {
        return UcPointsRegulationsAndSchedule7ActivitiesCondition.class;
    }

    @Override
    public Function<SscsCaseData, List<String>> getAnswersExtractor() {
        return getAllAnswersExtractor();
    }

    public static UcPointsRegulationsAndSchedule7ActivitiesCondition getTheSinglePassingPointsConditionForSubmittedActivitiesAndPoints(DecisionNoticeQuestionService questionService,
        SscsCaseData caseData) {
        for (UcPointsRegulationsAndSchedule7ActivitiesCondition ucPointsAndActivitiesCondition : UcPointsRegulationsAndSchedule7ActivitiesCondition.values()) {

            if (ucPointsAndActivitiesCondition.isApplicable(questionService, caseData) && ucPointsAndActivitiesCondition.getOptionalErrorMessage(questionService, caseData).isEmpty()) {
                return ucPointsAndActivitiesCondition;
            }
        }
        throw new IllegalStateException(
            "No points condition found for " + caseData.getSscsUcCaseData().getDoesSchedule8Paragraph4Apply() + ":" + caseData.getSscsUcCaseData().getSchedule7Selections() + ":" + caseData.getSscsUcCaseData().getSchedule9Paragraph4Selection());
    }

    public static Optional<UcAllowedOrRefusedCondition> getPassingAllowedOrRefusedCondition(DecisionNoticeQuestionService questionService,
        SscsCaseData caseData) {

        UcPointsRegulationsAndSchedule7ActivitiesCondition condition
            = getTheSinglePassingPointsConditionForSubmittedActivitiesAndPoints(questionService, caseData);

        if (condition.getOptionalErrorMessage(questionService, caseData).isEmpty()) {
            return Optional.of(UcAllowedOrRefusedCondition.getTheSinglePassingPointsConditionForSubmittedActivitiesAndPoints(questionService, caseData));
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
        return sscsCaseData -> CollectionUtils.collate(emptyIfNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionPhysicalDisabilitiesQuestion()),
            emptyIfNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionMentalAssessmentQuestion()));
    }
}
