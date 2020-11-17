package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AllowedOrRefusedPredicate.ALLOWED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AllowedOrRefusedPredicate.REFUSED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.StringListPredicate.EMPTY;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.StringListPredicate.NOT_EMPTY;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoPredicate.FALSE;
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

    REFUSED_NON_SUPPORT_GROUP_ONLY(
        isAllowedOrRefused(REFUSED),
        isWcaAppeal(TRUE, false),
        isSupportGroupOnly(YesNoPredicate.FALSE),
        isAnyPoints(),
        isAnySchedule7(),
        isPoints(POINTS_LESS_THAN_FIFTEEN),
        isRegulation29(FALSE),
        isSchedule7ActivitiesAnswer(StringListPredicate.UNSPECIFIED),
        isSchedule9Paragraph4(UNSPECIFIED)),
    REFUSED_SUPPORT_GROUP_ONLY_LOW_POINTS(
        isAllowedOrRefused(REFUSED),
        isWcaAppeal(TRUE, false),
        isSupportGroupOnly(YesNoPredicate.TRUE),
        isPoints(POINTS_LESS_THAN_FIFTEEN),
        isAnySchedule7(),
        isRegulation29(TRUE),
        isSchedule7ActivitiesAnswer(EMPTY),
        isSchedule9Paragraph4(FALSE)),
    REFUSED_SUPPORT_GROUP_ONLY_HIGH_POINTS(
        isAllowedOrRefused(REFUSED),
        isWcaAppeal(TRUE, false),
        isSupportGroupOnly(YesNoPredicate.TRUE),
        isPoints(POINTS_GREATER_OR_EQUAL_TO_FIFTEEN),
        isAnySchedule7(),
        isRegulation29(UNSPECIFIED),
        isSchedule7ActivitiesAnswer(EMPTY),
        isSchedule9Paragraph4(FALSE)),
    ALLOWED_NON_SUPPORT_GROUP_ONLY_HIGH_POINTS(
        isAllowedOrRefused(ALLOWED),
        isWcaAppeal(TRUE, false),
        isSupportGroupOnly(YesNoPredicate.FALSE),
        isPoints(POINTS_GREATER_OR_EQUAL_TO_FIFTEEN),
        isAnySchedule7()),
    ALLOWED_NON_SUPPORT_GROUP_ONLY_LOW_POINTS(
        isAllowedOrRefused(ALLOWED),
        isWcaAppeal(TRUE, false),
        isSupportGroupOnly(FALSE),
        isPoints(POINTS_LESS_THAN_FIFTEEN),
        isAnySchedule7(),
        isRegulation29(YesNoPredicate.TRUE)
    ),
    ALLOWED_SUPPORT_GROUP_ONLY_SCHEDULE_3_SELECTED(
        isAllowedOrRefused(ALLOWED),
        isWcaAppeal(TRUE, false),
        isSupportGroupOnly(TRUE),
        isAnyPoints(),
        isSchedule7(NOT_EMPTY),
        isRegulation29(TRUE.or(UNSPECIFIED))),
    ALLOWED_SUPPORT_GROUP_ONLY_SCHEDULE_3_NOT_SELECTED(
        isAllowedOrRefused(ALLOWED),
        isWcaAppeal(TRUE, false),
        isSupportGroupOnly(TRUE),
        isAnyPoints(),
        isSchedule7(EMPTY),
        isRegulation29(TRUE.or(UNSPECIFIED)),
        isSchedule9Paragraph4(YesNoPredicate.TRUE)),
    ALLOWED_SUPPORT_GROUP_ONLY_SCHEDULE_3_UNSPECIFIED(
        isAllowedOrRefused(ALLOWED),
        isWcaAppeal(TRUE,true),
        isSupportGroupOnly(TRUE),
        isAnyPoints(),
        isSchedule7(StringListPredicate.UNSPECIFIED),
        isRegulation29(TRUE.or(UNSPECIFIED)),
        isSchedule9Paragraph4(TRUE)),
    NON_WCA_APPEAL_ALLOWED(
            isAllowedOrRefused(ALLOWED),
            isWcaAppeal(FALSE, true),
            isSupportGroupOnly(TRUE.or(FALSE)),
            isAnyPoints(),
            isAnySchedule7(),
            isDwpReassessTheAward(TRUE)),
    NON_WCA_APPEAL_REFUSED(
            isAllowedOrRefused(REFUSED),
            isWcaAppeal(FALSE, true),
            isSupportGroupOnly(TRUE.or(FALSE)),
            isAnyPoints(),
            isAnySchedule7(),
            isDwpReassessTheAward(TRUE));

    Optional<UcPointsCondition> primaryPointsCondition;
    Optional<FieldCondition> schedule7ActivitiesSelected;
    List<FieldCondition> primaryConditions;
    List<FieldCondition> validationConditions;
    Optional<UcPointsCondition> validationPointsCondition;

    
    UcAllowedOrRefusedCondition(AllowedOrRefusedCondition allowedOrRefusedCondition, YesNoFieldCondition wcaAppealCondition, YesNoFieldCondition supportGroupOnlyCondition, Optional<UcPointsCondition> primaryPointsCondition, Optional<StringListPredicate> schedule7ActivitiesSelected,
        FieldCondition...validationConditions) {
        this(allowedOrRefusedCondition, wcaAppealCondition, supportGroupOnlyCondition, primaryPointsCondition, schedule7ActivitiesSelected, isAnyPoints(), validationConditions);
    }

    public UcScenario getEsaScenario(SscsCaseData caseData) {
        if (REFUSED_NON_SUPPORT_GROUP_ONLY == this) {
            return UcScenario.SCENARIO_1;
        } else if (REFUSED_SUPPORT_GROUP_ONLY_LOW_POINTS == this || REFUSED_SUPPORT_GROUP_ONLY_HIGH_POINTS == this) {
            return UcScenario.SCENARIO_2;
        } else if (ALLOWED_SUPPORT_GROUP_ONLY_SCHEDULE_3_NOT_SELECTED == this && isSchedule9Paragraph4(TRUE).isSatisified(caseData)) {
            return UcScenario.SCENARIO_3;
        } else if ((ALLOWED_SUPPORT_GROUP_ONLY_SCHEDULE_3_NOT_SELECTED == this && isSchedule9Paragraph4(UNSPECIFIED).isSatisified(caseData)) || ALLOWED_SUPPORT_GROUP_ONLY_SCHEDULE_3_SELECTED == this) {
            return UcScenario.SCENARIO_4;
        }  else if (ALLOWED_NON_SUPPORT_GROUP_ONLY_HIGH_POINTS == this && caseData.getSchedule7Selections().isEmpty()) {
            return UcScenario.SCENARIO_5;
        } else if (ALLOWED_NON_SUPPORT_GROUP_ONLY_HIGH_POINTS == this && !caseData.getSchedule7Selections().isEmpty()) {
            return UcScenario.SCENARIO_6;
        }  else if (ALLOWED_NON_SUPPORT_GROUP_ONLY_LOW_POINTS == this && caseData.getSchedule7Selections().isEmpty() && isSchedule9Paragraph4(FALSE).isSatisified(caseData)) {
            return UcScenario.SCENARIO_7;
        }  else if (ALLOWED_NON_SUPPORT_GROUP_ONLY_LOW_POINTS == this && caseData.getSchedule7Selections().isEmpty() && isSchedule9Paragraph4(TRUE).isSatisified(caseData)) {
            return UcScenario.SCENARIO_8;
        }  else if (ALLOWED_NON_SUPPORT_GROUP_ONLY_LOW_POINTS == this && !caseData.getSchedule7Selections().isEmpty()) {
            return UcScenario.SCENARIO_9;
        } else if (NON_WCA_APPEAL_ALLOWED == this || NON_WCA_APPEAL_REFUSED == this) {
            return UcScenario.SCENARIO_10;
        } else {
            throw new IllegalStateException("No scenario applicable");
        }
    }

    UcAllowedOrRefusedCondition(AllowedOrRefusedCondition allowedOrRefusedCondition, YesNoFieldCondition wcaAppealCondition, YesNoFieldCondition supportGroupOnlyCondition, Optional<UcPointsCondition> primaryPointsCondition, Optional<StringListPredicate> schedule3ActivitiesSelected, Optional<UcPointsCondition> validationPointsCondition,
        FieldCondition...validationConditions) {
        if (schedule3ActivitiesSelected.isPresent()) {
            this.schedule7ActivitiesSelected = Optional.of(isSchedule7ActivitiesAnswer(schedule3ActivitiesSelected.get()));
        } else {
            this.schedule7ActivitiesSelected = Optional.empty();
        }
        this.primaryPointsCondition = primaryPointsCondition;
        this.primaryConditions = new ArrayList<>();
        primaryConditions.add(allowedOrRefusedCondition);
        primaryConditions.add(wcaAppealCondition);
        primaryConditions.add(supportGroupOnlyCondition);
        if (this.schedule7ActivitiesSelected.isPresent()) {
            primaryConditions.add(this.schedule7ActivitiesSelected.get());
        }
        this.validationPointsCondition = validationPointsCondition;
        this.validationConditions = Arrays.asList(validationConditions);
    }

    static YesNoFieldCondition isRegulation29(Predicate<YesNo> predicate) {
        return new YesNoFieldCondition("Schedule 8 Paragraph 4", predicate,
                SscsCaseData::getDoesSchedule8Paragraph4Apply);
    }

    static YesNoFieldCondition isSupportGroupOnly(Predicate<YesNo> predicate) {
        return new YesNoFieldCondition("Support Group Only Appeal", predicate,
            s -> s.isSupportGroupOnlyAppeal() ? YesNo.YES : YesNo.NO);
    }

    static YesNoFieldCondition isWcaAppeal(Predicate<YesNo> predicate, boolean displayIsSatisfiedMessage) {
        return new YesNoFieldCondition("Wca Appeal", predicate,
            s -> s.isWcaAppeal() ? YesNo.YES : YesNo.NO, displayIsSatisfiedMessage);
    }

    static YesNoFieldCondition isDwpReassessTheAward(Predicate<YesNo> predicate) {
        return new YesNoFieldCondition("'When should DWP reassess the award?'", predicate,
            (SscsCaseData sscsCaseData) -> isNotBlank(sscsCaseData.getSscsUcCaseData().getDwpReassessTheAward()) ? YesNo.YES : YesNo.NO);
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
            SscsCaseData::getSchedule9Paragraph4Selection);
    }

    static FieldCondition isSchedule7ActivitiesAnswer(StringListPredicate predicate) {
        return new StringListFieldCondition("Schedule 7 Activities", predicate,
            SscsCaseData::getSchedule7Selections);
    }

    @Override
    public boolean isApplicable(DecisionNoticeQuestionService questionService, SscsCaseData caseData) {
        if ("Yes".equalsIgnoreCase(caseData.getWriteFinalDecisionGenerateNotice())) {
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

        for (UcAllowedOrRefusedCondition esaPointsAndActivitiesCondition : UcAllowedOrRefusedCondition.values()) {

            if (esaPointsAndActivitiesCondition.isApplicable(questionService, caseData) && esaPointsAndActivitiesCondition.getOptionalErrorMessage(questionService, caseData).isEmpty()) {
                return esaPointsAndActivitiesCondition;
            }
        }
        throw new IllegalStateException(
            "No points condition found for " + caseData.getDoesSchedule8Paragraph4Apply() + ":" + caseData.getSchedule7Selections() + ":" + caseData.getSchedule9Paragraph4Selection());
    }

    @Override
    public Optional<String> getOptionalErrorMessage(DecisionNoticeQuestionService questionService, SscsCaseData sscsCaseData) {

        final List<String> primaryCriteriaSatisfiedMessages =
            primaryConditions.stream()
                .map(FieldCondition::getOptionalIsSatisfiedMessage)
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
