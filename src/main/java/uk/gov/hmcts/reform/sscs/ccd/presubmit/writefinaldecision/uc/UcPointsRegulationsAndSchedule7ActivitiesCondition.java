package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
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

    LOW_POINTS_SCHEDULE_8_PARAGRAPH_4_UNSPECIFIED(UcPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        isWcaAppeal(TRUE), isSchedule8Paragraph4(UNSPECIFIED, false), Optional.empty(), isSchedule8Paragraph4(SPECIFIED)),
    LOW_POINTS_SCHEDULE_8_PARAGRAPH_4_DOES_NOT_APPLY(UcPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        isWcaAppeal(TRUE), isSchedule8Paragraph4(FALSE), Optional.of(AwardType.NO_AWARD), isSchedule9Paragraph4(UNSPECIFIED), isSchedule7ActivitiesAnswer(StringListPredicate.UNSPECIFIED)),
    LOW_POINTS_SCHEDULE_8_PARAGRAPH_4_DOES_APPLY_SCHEDULE_9_PARAGRAPH_4_UNSPECIFIED(UcPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isWcaAppeal(TRUE), isSchedule8Paragraph4(TRUE), isSchedule9Paragraph4(UNSPECIFIED)), Optional.of(AwardType.HIGHER_RATE),  isSchedule7ActivitiesAnswer(StringListPredicate.NOT_EMPTY)),
    LOW_POINTS_SCHEDULE_8_PARAGRAPH_4_DOES_APPLY_SCHEDULE_9_PARAGRAPH_4_DOES_NOT_APPLY(UcPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isWcaAppeal(TRUE), isSchedule8Paragraph4(TRUE), isSchedule9Paragraph4(FALSE)), Optional.of(AwardType.LOWER_RATE), isSchedule7ActivitiesAnswer(StringListPredicate.EMPTY)),
    LOW_POINTS_SCHEDULE_8_PARAGRAPH_4_DOES_APPLY_SCHEDULE_9_PARAGRAPH_4_DOES_APPLY(UcPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isWcaAppeal(TRUE), isSchedule8Paragraph4(TRUE), isSchedule9Paragraph4(TRUE)), Optional.of(AwardType.HIGHER_RATE), isSchedule7ActivitiesAnswer(StringListPredicate.EMPTY)),
    HIGH_POINTS_SCHEDULE_9_PARAGRAPH_4_UNSPECIFIED(UcPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN,
        isWcaAppeal(TRUE), isSchedule9Paragraph4(UNSPECIFIED), Optional.of(AwardType.HIGHER_RATE), isSchedule8Paragraph4(UNSPECIFIED), isSchedule7ActivitiesAnswer(StringListPredicate.NOT_EMPTY)),
    HIGH_POINTS_SCHEDULE_9_PARAGRAPH_4_DOES_NOT_APPLY(UcPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN,
        isWcaAppeal(TRUE),isSchedule9Paragraph4(FALSE), Optional.of(AwardType.LOWER_RATE),  isSchedule8Paragraph4(UNSPECIFIED), isSchedule7ActivitiesAnswer(StringListPredicate.EMPTY)),
    HIGH_POINTS_SCHEDULE_9_PARAGRAPH_4_DOES_APPLY(UcPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN,
        isWcaAppeal(TRUE), isSchedule9Paragraph4(TRUE), Optional.of(AwardType.HIGHER_RATE),  isSchedule8Paragraph4(UNSPECIFIED), isSchedule7ActivitiesAnswer(StringListPredicate.EMPTY)),
    NON_WCA_APPEAL(UcPointsCondition.POINTS_LESS_THAN_FIFTEEN,
            Arrays.asList(isWcaAppeal(FALSE)), Optional.empty(), isDwpReassessTheAward(TRUE));
    List<YesNoFieldCondition> primaryConditions;
    List<FieldCondition> validationConditions;

    UcPointsCondition pointsCondition;
    Optional<AwardType> awardType;

    UcPointsRegulationsAndSchedule7ActivitiesCondition(UcPointsCondition pointsCondition, YesNoFieldCondition primaryCondition1,YesNoFieldCondition primaryCondition2,
        Optional<AwardType> awardType, FieldCondition...validationConditions) {
        this(pointsCondition, Arrays.asList(primaryCondition1, primaryCondition2), awardType, validationConditions);
    }

    public Optional<AwardType> getAwardType() {
        return awardType;
    }

    UcPointsRegulationsAndSchedule7ActivitiesCondition(UcPointsCondition pointsCondition, List<YesNoFieldCondition> primaryConditions,
        Optional<AwardType> awardType, FieldCondition...validationConditions) {
        this.pointsCondition = pointsCondition;
        this.awardType = awardType;
        this.primaryConditions = primaryConditions;
        this.validationConditions = Arrays.asList(validationConditions);
    }

    static YesNoFieldCondition isSchedule8Paragraph4(YesNoPredicate predicate) {
        return new YesNoFieldCondition("Schedule 8 Paragraph 4", predicate,
                SscsCaseData::getDoesSchedule8Paragraph4Apply);
    }

    static YesNoFieldCondition isSchedule8Paragraph4(YesNoPredicate predicate, boolean displaySatisifiedMessageOnError) {
        return new YesNoFieldCondition("Schedule 8 Paragraph 4", predicate,
                SscsCaseData::getDoesSchedule8Paragraph4Apply, displaySatisifiedMessageOnError);
    }

    static YesNoFieldCondition isSchedule9Paragraph4(Predicate<YesNo> predicate) {
        return new YesNoFieldCondition("Schedule 9 Paragraph 4", predicate,
                SscsCaseData::getSchedule9Paragraph4Selection);
    }

    static YesNoFieldCondition isSchedule9Paragraph4(YesNoPredicate predicate, boolean displaySatisifiedMessageOnError) {
        return new YesNoFieldCondition("Schedule 9 Paragraph 4", predicate,
                SscsCaseData::getSchedule9Paragraph4Selection, displaySatisifiedMessageOnError);
    }

    static YesNoFieldCondition isWcaAppeal(Predicate<YesNo> predicate) {
        return new YesNoFieldCondition("Wca Appeal", predicate,
            sscsCaseData -> sscsCaseData.isWcaAppeal() ? YesNo.YES : YesNo.NO, false);
    }

    static YesNoFieldCondition isDwpReassessTheAward(Predicate<YesNo> predicate) {
        return new YesNoFieldCondition("'When should DWP reassess the award?'", predicate,
            (SscsCaseData sscsCaseData) -> isNotBlank(sscsCaseData.getSscsUcCaseData().getDwpReassessTheAward()) ? YesNo.YES : YesNo.NO);
    }

    static FieldCondition isSchedule7ActivitiesAnswer(StringListPredicate predicate) {
        return new StringListFieldCondition("Schedule 7 Activities", predicate,
            SscsCaseData::getSchedule7Selections);
    }

    @Override
    public boolean isApplicable(DecisionNoticeQuestionService questionService, SscsCaseData caseData) {
        int points = questionService.getTotalPoints(caseData, getAnswersExtractor().apply(caseData));
        return "Yes".equalsIgnoreCase(caseData.getWriteFinalDecisionGenerateNotice()) && pointsCondition.getPointsRequirementCondition().test(points) && primaryConditions.stream().allMatch(c -> c.isSatisified(caseData));
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
        for (UcPointsRegulationsAndSchedule7ActivitiesCondition esaPointsAndActivitiesCondition : UcPointsRegulationsAndSchedule7ActivitiesCondition.values()) {

            if (esaPointsAndActivitiesCondition.isApplicable(questionService, caseData) && esaPointsAndActivitiesCondition.getOptionalErrorMessage(questionService, caseData).isEmpty()) {
                return esaPointsAndActivitiesCondition;
            }
        }
        throw new IllegalStateException(
            "No points condition found for " + caseData.getDoesSchedule8Paragraph4Apply() + ":" + caseData.getSchedule7Selections() + ":" + caseData.getDoesSchedule9Paragraph4Apply());
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
        return sscsCaseData -> CollectionUtils.collate(emptyIfNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionPhysicalDisabilitiesQuestion()),
            emptyIfNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionMentalAssessmentQuestion()));
    }
}
