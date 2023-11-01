package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

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
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN;

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
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.scenarios.EsaScenario;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;
import uk.gov.hmcts.reform.sscs.utility.StringUtils;

/**
 * Encapsulates the conditions satisfied by valid combinations of allowed/refused and other
 * attributes of the Decision Notice journey - to be used on Outcome validation (eg. on submission),
 * but not on preview.
 */
public enum EsaAllowedOrRefusedCondition implements PointsCondition<EsaAllowedOrRefusedCondition> {

    // Scenario 1
    REFUSED_NON_SUPPORT_GROUP_ONLY(
        isAllowedOrRefused(REFUSED),
        isWcaAppeal(TRUE, false),
        isSupportGroupOnly(YesNoPredicate.NOT_TRUE, true),
        isAnyPoints(),
        isAnySchedule3(),
        isPoints(POINTS_LESS_THAN_FIFTEEN),
        isRegulation29(FALSE),
        isSchedule3ActivitiesAnswer(StringListPredicate.UNSPECIFIED),
        isSupportGroupOnly(YesNoPredicate.FALSE, false).get(),
        isRegulation35(UNSPECIFIED)),
    // Scenario 2
    REFUSED_SUPPORT_GROUP_ONLY_LOW_POINTS(
        isAllowedOrRefused(REFUSED),
        isWcaAppeal(TRUE, false),
        isSupportGroupOnly(YesNoPredicate.TRUE, true),
        isPoints(POINTS_LESS_THAN_FIFTEEN),
        isAnySchedule3(),
        isRegulation29(UNSPECIFIED),
        isSchedule3ActivitiesAnswer(EMPTY),
        isRegulation35(FALSE)),
    // Scenario 2
    REFUSED_SUPPORT_GROUP_ONLY_HIGH_POINTS(
        isAllowedOrRefused(REFUSED),
        isWcaAppeal(TRUE, false),
        isSupportGroupOnly(YesNoPredicate.TRUE, true),
        isPoints(POINTS_GREATER_OR_EQUAL_TO_FIFTEEN),
        isAnySchedule3(),
        isRegulation29(UNSPECIFIED),
        isSchedule3ActivitiesAnswer(EMPTY),
        isRegulation35(FALSE)),
    // Scenario 5 and Scenario 6
    ALLOWED_NON_SUPPORT_GROUP_ONLY_HIGH_POINTS(
        isAllowedOrRefused(ALLOWED),
        isWcaAppeal(TRUE, false),
        isSupportGroupOnly(YesNoPredicate.NOT_TRUE, true),
        isPoints(POINTS_GREATER_OR_EQUAL_TO_FIFTEEN),
        isAnySchedule3(),
        isSupportGroupOnly(YesNoPredicate.FALSE, false).get()),
    // Scenario 7 and Scenario 8
    ALLOWED_NON_SUPPORT_GROUP_ONLY_LOW_POINTS(
        isAllowedOrRefused(ALLOWED),
        isWcaAppeal(TRUE, false),
        isSupportGroupOnly(NOT_TRUE, true),
        isPoints(POINTS_LESS_THAN_FIFTEEN),
        isAnySchedule3(),
        isSupportGroupOnly(YesNoPredicate.FALSE, false).get(),
        isRegulation29(YesNoPredicate.TRUE)
    ),
    // Scenario 4
    ALLOWED_SUPPORT_GROUP_ONLY_SCHEDULE_3_SELECTED(
        isAllowedOrRefused(ALLOWED),
        isWcaAppeal(TRUE, false),
        isSupportGroupOnly(TRUE, true),
        isAnyPoints(),
        isSchedule3(NOT_EMPTY),
        isRegulation29(UNSPECIFIED)),
    // SCENARIO_3
    ALLOWED_SUPPORT_GROUP_ONLY_SCHEDULE_3_NOT_SELECTED(
        isAllowedOrRefused(ALLOWED),
        isWcaAppeal(TRUE, false),
        isSupportGroupOnly(TRUE, true),
        isAnyPoints(),
        isSchedule3(EMPTY),
        isRegulation29(UNSPECIFIED),
        isRegulation35(YesNoPredicate.TRUE)),
    ALLOWED_SUPPORT_GROUP_ONLY_SCHEDULE_3_UNSPECIFIED(
        isAllowedOrRefused(ALLOWED),
        isWcaAppeal(TRUE,true),
        isSupportGroupOnly(TRUE, true),
        isAnyPoints(),
        isSchedule3(StringListPredicate.UNSPECIFIED),
        isRegulation29(UNSPECIFIED),
        isRegulation35(TRUE)),
    // Scenario 10
    NON_WCA_APPEAL_ALLOWED(
            isAllowedOrRefused(ALLOWED),
            isWcaAppeal(FALSE, true),
            isAnySupportGroupOnly(),
            isAnyPoints(),
            isAnySchedule3(),
            isDwpReassessTheAward(UNSPECIFIED)),
    // Scenario 10
    NON_WCA_APPEAL_REFUSED(
            isAllowedOrRefused(REFUSED),
            isWcaAppeal(FALSE, true),
            isAnySupportGroupOnly(),
            isAnyPoints(),
            isAnySchedule3(),
            isDwpReassessTheAward(UNSPECIFIED));

    Optional<EsaPointsCondition> primaryPointsCondition;
    Optional<FieldCondition> schedule3ActivitiesSelected;
    List<FieldCondition> primaryConditions;
    List<FieldCondition> validationConditions;
    Optional<EsaPointsCondition> validationPointsCondition;


    EsaAllowedOrRefusedCondition(AllowedOrRefusedCondition allowedOrRefusedCondition, YesNoFieldCondition wcaAppealCondition, Optional<YesNoFieldCondition> supportGroupOnlyCondition, Optional<EsaPointsCondition> primaryPointsCondition, Optional<StringListPredicate> schedule3ActivitiesSelected,
        FieldCondition...validationConditions) {
        this(allowedOrRefusedCondition, wcaAppealCondition, supportGroupOnlyCondition, primaryPointsCondition, schedule3ActivitiesSelected, isAnyPoints(), validationConditions);
    }

    public EsaScenario getEsaScenario(SscsCaseData caseData) {
        if (REFUSED_NON_SUPPORT_GROUP_ONLY == this) {
            return EsaScenario.SCENARIO_1;
        } else if (REFUSED_SUPPORT_GROUP_ONLY_LOW_POINTS == this || REFUSED_SUPPORT_GROUP_ONLY_HIGH_POINTS == this) {
            return EsaScenario.SCENARIO_2;
        } else if (ALLOWED_SUPPORT_GROUP_ONLY_SCHEDULE_3_NOT_SELECTED == this && isRegulation35(TRUE).isSatisified(caseData)) {
            return EsaScenario.SCENARIO_3;
        } else if ((ALLOWED_SUPPORT_GROUP_ONLY_SCHEDULE_3_NOT_SELECTED == this && isRegulation35(UNSPECIFIED).isSatisified(caseData)) || ALLOWED_SUPPORT_GROUP_ONLY_SCHEDULE_3_SELECTED == this) {
            return EsaScenario.SCENARIO_4;
        }  else if (ALLOWED_NON_SUPPORT_GROUP_ONLY_HIGH_POINTS == this && caseData.getSscsEsaCaseData().getSchedule3Selections().isEmpty()) {
            if (isRegulation35(TRUE).isSatisified(caseData)) {
                return EsaScenario.SCENARIO_12;
            } else {
                return EsaScenario.SCENARIO_5;
            }
        } else if (ALLOWED_NON_SUPPORT_GROUP_ONLY_HIGH_POINTS == this && !caseData.getSscsEsaCaseData().getSchedule3Selections().isEmpty()) {
            return EsaScenario.SCENARIO_6;
        }  else if (ALLOWED_NON_SUPPORT_GROUP_ONLY_LOW_POINTS == this && caseData.getSscsEsaCaseData().getSchedule3Selections().isEmpty() && isRegulation35(FALSE).isSatisified(caseData)) {
            return EsaScenario.SCENARIO_7;
        }  else if (ALLOWED_NON_SUPPORT_GROUP_ONLY_LOW_POINTS == this && caseData.getSscsEsaCaseData().getSchedule3Selections().isEmpty() && isRegulation35(TRUE).isSatisified(caseData)) {
            return EsaScenario.SCENARIO_8;
        }  else if (ALLOWED_NON_SUPPORT_GROUP_ONLY_LOW_POINTS == this && !caseData.getSscsEsaCaseData().getSchedule3Selections().isEmpty()) {
            return EsaScenario.SCENARIO_9;
        } else if (NON_WCA_APPEAL_ALLOWED == this || NON_WCA_APPEAL_REFUSED == this) {
            return EsaScenario.SCENARIO_10;
        } else {
            throw new IllegalStateException("No scenario applicable");
        }
    }

    EsaAllowedOrRefusedCondition(AllowedOrRefusedCondition allowedOrRefusedCondition, YesNoFieldCondition wcaAppealCondition, Optional<YesNoFieldCondition> supportGroupOnlyCondition, Optional<EsaPointsCondition> primaryPointsCondition, Optional<StringListPredicate> schedule3ActivitiesSelected, Optional<EsaPointsCondition> validationPointsCondition,
        FieldCondition...validationConditions) {
        if (schedule3ActivitiesSelected.isPresent()) {
            this.schedule3ActivitiesSelected = Optional.of(isSchedule3ActivitiesAnswer(schedule3ActivitiesSelected.get()));
        } else {
            this.schedule3ActivitiesSelected = Optional.empty();
        }
        this.primaryPointsCondition = primaryPointsCondition;
        this.primaryConditions = new ArrayList<>();
        primaryConditions.add(allowedOrRefusedCondition);
        primaryConditions.add(wcaAppealCondition);
        if (supportGroupOnlyCondition.isPresent()) {
            primaryConditions.add(supportGroupOnlyCondition.get());
        }
        if (this.schedule3ActivitiesSelected.isPresent()) {
            primaryConditions.add(this.schedule3ActivitiesSelected.get());
        }
        this.validationPointsCondition = validationPointsCondition;
        this.validationConditions = Arrays.asList(validationConditions);
    }

    static YesNoFieldCondition isRegulation29(Predicate<YesNo> predicate) {
        return new YesNoFieldCondition("Regulation 29", predicate,
            caseData -> caseData.getSscsEsaCaseData().getDoesRegulation29Apply());
    }

    static Optional<YesNoFieldCondition> isSupportGroupOnly(Predicate<YesNo> predicate, boolean displayPointsSatisfiedMessageOnError) {
        return Optional.of(new YesNoFieldCondition("Support Group Only Appeal", predicate,
            s -> s.getSupportGroupOnlyAppeal() == null ? null : s.isSupportGroupOnlyAppeal() ? YesNo.YES : YesNo.NO, displayPointsSatisfiedMessageOnError));
    }

    static Optional<YesNoFieldCondition> isAnySupportGroupOnly() {
        return Optional.empty();
    }

    static YesNoFieldCondition isWcaAppeal(Predicate<YesNo> predicate, boolean displayIsSatisfiedMessage) {
        return new YesNoFieldCondition("Wca Appeal", predicate,
            s -> s.isWcaAppeal() ? YesNo.YES : YesNo.NO, displayIsSatisfiedMessage);
    }

    static YesNoFieldCondition isDwpReassessTheAward(Predicate<YesNo> predicate) {
        return new YesNoFieldCondition("'When should DWP reassess the award?'", predicate,
            (SscsCaseData sscsCaseData) -> sscsCaseData.getDwpReassessTheAward() == null ? null : isNotBlank(sscsCaseData.getDwpReassessTheAward()) ? YesNo.YES : YesNo.NO);
    }

    static Optional<EsaPointsCondition> isAnyPoints() {
        return Optional.empty();
    }

    static Optional<StringListPredicate> isSchedule3(StringListPredicate p) {
        return Optional.of(p);
    }

    static Optional<StringListPredicate> isAnySchedule3() {
        return Optional.empty();
    }

    static Optional<EsaPointsCondition> isPoints(EsaPointsCondition pointsCondition) {
        return Optional.of(pointsCondition);
    }

    static AllowedOrRefusedCondition isAllowedOrRefused(AllowedOrRefusedPredicate predicate) {
        return new AllowedOrRefusedCondition(predicate);
    }

    static YesNoFieldCondition isRegulation35(YesNoPredicate predicate) {
        return new YesNoFieldCondition("Regulation 35", predicate,
            caseData -> caseData.getSscsEsaCaseData().getRegulation35Selection());
    }

    static FieldCondition isSchedule3ActivitiesAnswer(StringListPredicate predicate) {
        return new StringListFieldCondition("Schedule 3 Activities", predicate,
            caseData -> caseData.getSscsEsaCaseData().getSchedule3Selections());
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
    public Class<EsaAllowedOrRefusedCondition> getEnumClass() {
        return EsaAllowedOrRefusedCondition.class;
    }

    @Override
    public Function<SscsCaseData, List<String>> getAnswersExtractor() {
        return getAllAnswersExtractor();
    }

    protected static EsaAllowedOrRefusedCondition getTheSinglePassingPointsConditionForSubmittedActivitiesAndPoints(DecisionNoticeQuestionService questionService,
        SscsCaseData caseData) {

        for (EsaAllowedOrRefusedCondition esaPointsAndActivitiesCondition : EsaAllowedOrRefusedCondition.values()) {

            if (esaPointsAndActivitiesCondition.isApplicable(questionService, caseData) && esaPointsAndActivitiesCondition.getOptionalErrorMessage(questionService, caseData).isEmpty()) {
                return esaPointsAndActivitiesCondition;
            }
        }
        throw new IllegalStateException(
            "No allowed/refused condition found for " + caseData.getSscsEsaCaseData().getDoesRegulation29Apply() + ":" + caseData.getSscsEsaCaseData().getSchedule3Selections() + ":" + caseData.getSscsEsaCaseData().getRegulation35Selection());
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
        return sscsCaseData -> CollectionUtils.collate(emptyIfNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionPhysicalDisabilitiesQuestion()),
            emptyIfNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionMentalAssessmentQuestion()));
    }
}
