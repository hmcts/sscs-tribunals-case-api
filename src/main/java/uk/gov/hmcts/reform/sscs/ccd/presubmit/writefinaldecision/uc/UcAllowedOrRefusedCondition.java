package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AllowedOrRefusedPredicate.ALLOWED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AllowedOrRefusedPredicate.REFUSED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.StringListPredicate.EMPTY;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.StringListPredicate.NOT_EMPTY;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoPredicate.FALSE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoPredicate.NOT_TRUE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoPredicate.TRUE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoPredicate.UNSPECIFIED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.UcPointsCondition.POINTS_LESS_THAN_FIFTEEN;

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
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AllowedOrRefusedCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AllowedOrRefusedPredicate;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.FieldCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.PointsCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.StringListFieldCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.StringListPredicate;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoFieldCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoPredicate;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc.scenarios.UcScenario;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;
import uk.gov.hmcts.reform.sscs.utility.StringUtils;

/**
 * Encapsulates the conditions satisfied by valid combinations of allowed/refused and other
 * attributes of the Decision Notice journey - to be used on Outcome validation (eg. on submission),
 * but not on preview.
 */
public enum UcAllowedOrRefusedCondition implements PointsCondition<UcAllowedOrRefusedCondition> {

    // Scenario 1
    REFUSED_NON_SUPPORT_GROUP_ONLY(
        isAllowedOrRefused(REFUSED),
        isWcaAppeal(TRUE, false),
        isSupportGroupOnly(YesNoPredicate.NOT_TRUE, true),
        isAnyPoints(),
        isAnySchedule7(),
        isPoints(POINTS_LESS_THAN_FIFTEEN),
        isSchdeul8Paragraph4(FALSE),
        isSchedule7ActivitiesAnswer(StringListPredicate.UNSPECIFIED),
        isSupportGroupOnly(YesNoPredicate.FALSE, false).get(),
        isSchedule9Paragraph4(UNSPECIFIED)),
    // Scenario 2
    REFUSED_SUPPORT_GROUP_ONLY_LOW_POINTS(
        isAllowedOrRefused(REFUSED),
        isWcaAppeal(TRUE, false),
        isSupportGroupOnly(YesNoPredicate.TRUE, true),
        isPoints(POINTS_LESS_THAN_FIFTEEN),
        isAnySchedule7(),
        isSchdeul8Paragraph4(UNSPECIFIED),
        isSchedule7ActivitiesAnswer(EMPTY),
        isSchedule9Paragraph4(FALSE)),
    // Scenario 2
    REFUSED_SUPPORT_GROUP_ONLY_HIGH_POINTS(
        isAllowedOrRefused(REFUSED),
        isWcaAppeal(TRUE, false),
        isSupportGroupOnly(YesNoPredicate.TRUE, true),
        isPoints(POINTS_GREATER_OR_EQUAL_TO_FIFTEEN),
        isAnySchedule7(),
        isSchdeul8Paragraph4(UNSPECIFIED),
        isSchedule7ActivitiesAnswer(EMPTY),
        isSchedule9Paragraph4(FALSE)),
    // Scenario 5 and Scenario 6
    ALLOWED_NON_SUPPORT_GROUP_ONLY_HIGH_POINTS(
        isAllowedOrRefused(ALLOWED),
        isWcaAppeal(TRUE, false),
        isSupportGroupOnly(YesNoPredicate.NOT_TRUE, true),
        isPoints(POINTS_GREATER_OR_EQUAL_TO_FIFTEEN),
        isAnySchedule7(),
        isSupportGroupOnly(YesNoPredicate.FALSE, false).get()),
    // Scenario 7 and Scenario 8
    ALLOWED_NON_SUPPORT_GROUP_ONLY_LOW_POINTS(
        isAllowedOrRefused(ALLOWED),
        isWcaAppeal(TRUE, false),
        isSupportGroupOnly(NOT_TRUE, true),
        isPoints(POINTS_LESS_THAN_FIFTEEN),
        isAnySchedule7(),
        isSupportGroupOnly(YesNoPredicate.FALSE, false).get(),
        isSchdeul8Paragraph4(YesNoPredicate.TRUE)
    ),
    // Scenario 4
    ALLOWED_SUPPORT_GROUP_ONLY_SCHEDULE_3_SELECTED(
        isAllowedOrRefused(ALLOWED),
        isWcaAppeal(TRUE, false),
        isSupportGroupOnly(TRUE, true),
        isAnyPoints(),
        isSchedule7(NOT_EMPTY),
        isSchdeul8Paragraph4(UNSPECIFIED)),
    // SCENARIO_3
    ALLOWED_SUPPORT_GROUP_ONLY_SCHEDULE_3_NOT_SELECTED(
        isAllowedOrRefused(ALLOWED),
        isWcaAppeal(TRUE, false),
        isSupportGroupOnly(TRUE, true),
        isAnyPoints(),
        isSchedule7(EMPTY),
        isSchdeul8Paragraph4(UNSPECIFIED),
        isSchedule9Paragraph4(YesNoPredicate.TRUE)),
    ALLOWED_SUPPORT_GROUP_ONLY_SCHEDULE_3_UNSPECIFIED(
        isAllowedOrRefused(ALLOWED),
        isWcaAppeal(TRUE,true),
        isSupportGroupOnly(TRUE, true),
        isAnyPoints(),
        isSchedule7(StringListPredicate.UNSPECIFIED),
        isSchdeul8Paragraph4(UNSPECIFIED),
        isSchedule9Paragraph4(TRUE)),
    // Scenario 10
    NON_WCA_APPEAL_ALLOWED(
            isAllowedOrRefused(ALLOWED),
            isWcaAppeal(FALSE, true),
            isAnySupportGroupOnly(),
            isAnyPoints(),
            isAnySchedule7(),
            isDwpReassessTheAward(UNSPECIFIED)),
    // Scenario 10
    NON_WCA_APPEAL_REFUSED(
            isAllowedOrRefused(REFUSED),
            isWcaAppeal(FALSE, true),
            isAnySupportGroupOnly(),
            isAnyPoints(),
            isAnySchedule7(),
            isDwpReassessTheAward(UNSPECIFIED));

    Optional<UcPointsCondition> primaryPointsCondition;
    Optional<FieldCondition> schedule7ActivitiesSelected;
    List<FieldCondition> primaryConditions;
    List<FieldCondition> validationConditions;
    Optional<UcPointsCondition> validationPointsCondition;


    UcAllowedOrRefusedCondition(
        AllowedOrRefusedCondition allowedOrRefusedCondition, YesNoFieldCondition wcaAppealCondition, Optional<YesNoFieldCondition> supportGroupOnlyCondition, Optional<UcPointsCondition> primaryPointsCondition, Optional<StringListPredicate> schedule7ActivitiesSelected,
        FieldCondition...validationConditions) {
        this(allowedOrRefusedCondition, wcaAppealCondition, supportGroupOnlyCondition, primaryPointsCondition, schedule7ActivitiesSelected, isAnyPoints(), validationConditions);
    }

    public UcScenario getUcScenario(SscsCaseData caseData) {
        if (REFUSED_NON_SUPPORT_GROUP_ONLY == this) {
            return UcScenario.SCENARIO_1;
        } else if (REFUSED_SUPPORT_GROUP_ONLY_LOW_POINTS == this || REFUSED_SUPPORT_GROUP_ONLY_HIGH_POINTS == this) {
            return UcScenario.SCENARIO_2;
        } else if (ALLOWED_SUPPORT_GROUP_ONLY_SCHEDULE_3_NOT_SELECTED == this && isSchedule9Paragraph4(TRUE).isSatisified(caseData)) {
            return UcScenario.SCENARIO_3;
        } else if ((ALLOWED_SUPPORT_GROUP_ONLY_SCHEDULE_3_NOT_SELECTED == this && isSchedule9Paragraph4(UNSPECIFIED).isSatisified(caseData)) || ALLOWED_SUPPORT_GROUP_ONLY_SCHEDULE_3_SELECTED == this) {
            return UcScenario.SCENARIO_4;
        }  else if (ALLOWED_NON_SUPPORT_GROUP_ONLY_HIGH_POINTS == this && caseData.getSscsUcCaseData().getSchedule7Selections().isEmpty()) {
            if (isSchedule9Paragraph4(TRUE).isSatisified(caseData)) {
                return UcScenario.SCENARIO_12;
            } else {
                return UcScenario.SCENARIO_5;
            }
        } else if (ALLOWED_NON_SUPPORT_GROUP_ONLY_HIGH_POINTS == this && !caseData.getSscsUcCaseData().getSchedule7Selections().isEmpty()) {
            return UcScenario.SCENARIO_6;
        }  else if (ALLOWED_NON_SUPPORT_GROUP_ONLY_LOW_POINTS == this && caseData.getSscsUcCaseData().getSchedule7Selections().isEmpty() && isSchedule9Paragraph4(FALSE).isSatisified(caseData)) {
            return UcScenario.SCENARIO_7;
        }  else if (ALLOWED_NON_SUPPORT_GROUP_ONLY_LOW_POINTS == this && caseData.getSscsUcCaseData().getSchedule7Selections().isEmpty() && isSchedule9Paragraph4(TRUE).isSatisified(caseData)) {
            return UcScenario.SCENARIO_8;
        }  else if (ALLOWED_NON_SUPPORT_GROUP_ONLY_LOW_POINTS == this && !caseData.getSscsUcCaseData().getSchedule7Selections().isEmpty()) {
            return UcScenario.SCENARIO_9;
        } else if (NON_WCA_APPEAL_ALLOWED == this || NON_WCA_APPEAL_REFUSED == this) {
            return UcScenario.SCENARIO_10;
        } else {
            throw new IllegalStateException("No scenario applicable");
        }
    }

    UcAllowedOrRefusedCondition(
        AllowedOrRefusedCondition allowedOrRefusedCondition, YesNoFieldCondition wcaAppealCondition, Optional<YesNoFieldCondition> supportGroupOnlyCondition, Optional<UcPointsCondition> primaryPointsCondition, Optional<StringListPredicate> schedule7ActivitiesSelected, Optional<UcPointsCondition> validationPointsCondition,
        FieldCondition...validationConditions) {
        if (schedule7ActivitiesSelected.isPresent()) {
            this.schedule7ActivitiesSelected = Optional.of(isSchedule7ActivitiesAnswer(schedule7ActivitiesSelected.get()));
        } else {
            this.schedule7ActivitiesSelected = Optional.empty();
        }
        this.primaryPointsCondition = primaryPointsCondition;
        this.primaryConditions = new ArrayList<>();
        primaryConditions.add(allowedOrRefusedCondition);
        primaryConditions.add(wcaAppealCondition);
        if (supportGroupOnlyCondition.isPresent()) {
            primaryConditions.add(supportGroupOnlyCondition.get());
        }
        if (this.schedule7ActivitiesSelected.isPresent()) {
            primaryConditions.add(this.schedule7ActivitiesSelected.get());
        }
        this.validationPointsCondition = validationPointsCondition;
        this.validationConditions = Arrays.asList(validationConditions);
    }

    static YesNoFieldCondition isSchdeul8Paragraph4(Predicate<YesNo> predicate) {
        return new YesNoFieldCondition("Schedule 8 Paragraph 4", predicate,
            caseData -> caseData.getSscsUcCaseData().getDoesSchedule8Paragraph4Apply());
    }

    static Optional<YesNoFieldCondition> isSupportGroupOnly(Predicate<YesNo> predicate, boolean displayPointsSatisfiedMessageOnError) {
        return Optional.of(new YesNoFieldCondition("Support Group Only Appeal", predicate,
            s -> s.getSupportGroupOnlyAppeal() == null ? null : s.isSupportGroupOnlyAppeal() ? YesNo.YES : YesNo.NO, displayPointsSatisfiedMessageOnError));
    }

    static Optional<YesNoFieldCondition> isAnySupportGroupOnly() {
        return Optional.empty();
    }

    static YesNoFieldCondition isWcaAppeal(Predicate<YesNo> predicate, boolean displayIsSatisfiedMessage) {
        return new YesNoFieldCondition("WCA Appeal", predicate,
            s -> s.isWcaAppeal() ? YesNo.YES : YesNo.NO, displayIsSatisfiedMessage);
    }

    static YesNoFieldCondition isDwpReassessTheAward(Predicate<YesNo> predicate) {
        return new YesNoFieldCondition("'When should DWP reassess the award?'", predicate,
            (SscsCaseData sscsCaseData) -> sscsCaseData.getDwpReassessTheAward() == null ? null : isNotBlank(sscsCaseData.getDwpReassessTheAward()) ? YesNo.YES : YesNo.NO);
    }

    static Optional<UcPointsCondition> isAnyPoints() {
        return Optional.empty();
    }

    static Optional<StringListPredicate> isSchedule7(StringListPredicate p) {
        return Optional.of(p);
    }

    static Optional<StringListPredicate> isAnySchedule7() {
        return Optional.empty();
    }

    static Optional<UcPointsCondition> isPoints(UcPointsCondition pointsCondition) {
        return Optional.of(pointsCondition);
    }

    static AllowedOrRefusedCondition isAllowedOrRefused(AllowedOrRefusedPredicate predicate) {
        return new AllowedOrRefusedCondition(predicate);
    }

    static YesNoFieldCondition isSchedule9Paragraph4(YesNoPredicate predicate) {
        return new YesNoFieldCondition("Schedule 9 Paragraph 4", predicate,
            caseData -> caseData.getSscsUcCaseData().getSchedule9Paragraph4Selection());
    }

    static FieldCondition isSchedule7ActivitiesAnswer(StringListPredicate predicate) {
        return new StringListFieldCondition("Schedule 7 Activities", predicate,
            caseData -> caseData.getSscsUcCaseData().getSchedule7Selections());
    }

    @Override
    public boolean isApplicable(DecisionNoticeQuestionService questionService, SscsCaseData caseData) {
        if (isYes(caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGenerateNotice())) {
            int points = questionService.getTotalPoints(caseData, getAnswersExtractor().apply(caseData));
            if (primaryPointsCondition.isPresent()) {
                if (!primaryPointsCondition.get().getPointsRequirementCondition().test(points)) {
                    return false;
                } else {
                    return primaryConditions.stream().allMatch(c -> c.isSatisified(caseData));
                }
            } else {
                return primaryConditions.stream().allMatch(c -> c.isSatisified(caseData));
            }
        } else {
            return false;
        }
    }

    @Override
    public IntPredicate getPointsRequirementCondition() {
        return p -> (primaryPointsCondition.isPresent() ? primaryPointsCondition.get().getPointsRequirementCondition().test(p) : true);
    }

    @Override
    public Class<UcAllowedOrRefusedCondition> getEnumClass() {
        return UcAllowedOrRefusedCondition.class;
    }

    @Override
    public Function<SscsCaseData, List<String>> getAnswersExtractor() {
        return getAllAnswersExtractor();
    }

    protected static UcAllowedOrRefusedCondition getTheSinglePassingPointsConditionForSubmittedActivitiesAndPoints(DecisionNoticeQuestionService questionService,
        SscsCaseData caseData) {

        for (UcAllowedOrRefusedCondition ucPointsAndActivitiesCondition : UcAllowedOrRefusedCondition.values()) {

            if (ucPointsAndActivitiesCondition.isApplicable(questionService, caseData) && ucPointsAndActivitiesCondition.getOptionalErrorMessage(questionService, caseData).isEmpty()) {
                return ucPointsAndActivitiesCondition;
            }
        }
        throw new IllegalStateException(
            "No allowed/refused condition found for " + caseData.getSscsUcCaseData().getDoesSchedule8Paragraph4Apply() + ":" + caseData.getSscsUcCaseData().getSchedule7Selections() + ":" + caseData.getSscsUcCaseData().getSchedule9Paragraph4Selection());
    }

    @Override
    public Optional<String> getOptionalErrorMessage(DecisionNoticeQuestionService questionService, SscsCaseData sscsCaseData) {

        final List<String> primaryCriteriaSatisfiedMessages =
            primaryConditions.stream()
                .map(c -> c.getOptionalIsSatisfiedMessage(sscsCaseData))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        final List<String> validationErrorMessages =
                validationConditions.stream()
                .map(e -> e.getOptionalErrorMessage(sscsCaseData))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        List<String> criteriaSatisfiedMessages = new ArrayList<>();
        if (primaryPointsCondition.isPresent()) {
            criteriaSatisfiedMessages.add(primaryPointsCondition.get().getIsSatisfiedMessage());
        }
        criteriaSatisfiedMessages.addAll(primaryCriteriaSatisfiedMessages);

        List<String> validationMessages = new ArrayList<>();
        if (validationPointsCondition.isPresent()) {
            int points = questionService.getTotalPoints(sscsCaseData, getAnswersExtractor().apply(sscsCaseData));
            if (!validationPointsCondition.get().getPointsRequirementCondition().test(points)) {
                validationMessages.add(validationPointsCondition.get().getErrorMessage());
            }
        }
        validationMessages.addAll(validationErrorMessages);

        if (!validationMessages.isEmpty()) {
            return Optional.of("You have " + StringUtils.getGramaticallyJoinedStrings(criteriaSatisfiedMessages)
                    + (criteriaSatisfiedMessages.isEmpty() ? "" : ", but have ") + StringUtils.getGramaticallyJoinedStrings(validationMessages)
               + ". Please review your previous selection.");
        }
        return Optional.empty();
    }

    public static Function<SscsCaseData, List<String>> getAllAnswersExtractor() {
        return sscsCaseData -> CollectionUtils.collate(emptyIfNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionPhysicalDisabilitiesQuestion()),
            emptyIfNull(sscsCaseData.getSscsUcCaseData().getUcWriteFinalDecisionMentalAssessmentQuestion()));
    }
}
