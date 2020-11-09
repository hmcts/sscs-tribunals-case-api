package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AllowedOrRefusedPredicate.ALLOWED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AllowedOrRefusedPredicate.REFUSED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.StringListPredicate.EMPTY;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.StringListPredicate.NOT_EMPTY;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoPredicate.FALSE;
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
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;
import uk.gov.hmcts.reform.sscs.utility.StringUtils;

/**
 * Encapsulates the conditions satisfied by valid combinations of allowed/refused and other
 * attributes of the Decision Notice journey - to be used on Outcome validation (eg. on submission),
 * but not on preview.
 */
public enum EsaAllowedOrRefusedCondition implements PointsCondition<EsaAllowedOrRefusedCondition> {

    REFUSED_NON_SUPPORT_GROUP_ONLY(
        isAllowedOrRefused(REFUSED),
        isSupportGroupOnly(YesNoPredicate.FALSE),
        isAnyPoints(),
        isAnySchedule3(),
        isPoints(POINTS_LESS_THAN_FIFTEEN),
        isRegulation29(FALSE),
        isSchedule3ActivitiesAnswer(StringListPredicate.UNSPECIFIED),
        isRegulation35(UNSPECIFIED)),
    REFUSED_SUPPORT_GROUP_ONLY_LOW_POINTS(
        isAllowedOrRefused(REFUSED),
        isSupportGroupOnly(YesNoPredicate.TRUE),
        isPoints(POINTS_LESS_THAN_FIFTEEN),
        isAnySchedule3(),
        isRegulation29(TRUE),
        isSchedule3ActivitiesAnswer(EMPTY),
        isRegulation35(FALSE)),
    REFUSED_SUPPORT_GROUP_ONLY_HIGH_POINTS(
        isAllowedOrRefused(REFUSED),
        isSupportGroupOnly(YesNoPredicate.TRUE),
        isPoints(POINTS_GREATER_OR_EQUAL_TO_FIFTEEN),
        isAnySchedule3(),
        isRegulation29(UNSPECIFIED),
        isSchedule3ActivitiesAnswer(EMPTY),
        isRegulation35(FALSE)),
    ALLOWED_NON_SUPPORT_GROUP_ONLY_HIGH_POINTS(
        isAllowedOrRefused(ALLOWED),
        isSupportGroupOnly(YesNoPredicate.FALSE),
        isPoints(POINTS_GREATER_OR_EQUAL_TO_FIFTEEN),
        isAnySchedule3()),
    ALLOWED_NON_SUPPORT_GROUP_ONLY_LOW_POINTS(
        isAllowedOrRefused(ALLOWED),
        isSupportGroupOnly(FALSE),
        isPoints(POINTS_LESS_THAN_FIFTEEN),
        isAnySchedule3(),
        isRegulation29(YesNoPredicate.TRUE)
    ),
    ALLOWED_SUPPORT_GROUP_ONLY_SCHEDULE_3_SELECTED(
        isAllowedOrRefused(ALLOWED),
        isSupportGroupOnly(TRUE),
        isAnyPoints(),
        isSchedule3(NOT_EMPTY),
        isRegulation29(TRUE.or(UNSPECIFIED))),
    ALLOWED_SUPPORT_GROUP_ONLY_SCHEDULE_3_NOT_SELECTED(
        isAllowedOrRefused(ALLOWED),
        isSupportGroupOnly(TRUE),
        isAnyPoints(),
        isSchedule3(EMPTY),
        isRegulation29(TRUE.or(UNSPECIFIED)),
        isRegulation35(YesNoPredicate.TRUE)),
    ALLOWED_SUPPORT_GROUP_ONLY_SCHEDULE_3_UNSPECIFIED(
        isAllowedOrRefused(ALLOWED),
        isSupportGroupOnly(TRUE),
        isAnyPoints(),
        isSchedule3(StringListPredicate.UNSPECIFIED),
        isRegulation29(TRUE.or(UNSPECIFIED)),
    isRegulation35(TRUE));

    Optional<EsaPointsCondition> primaryPointsCondition;
    Optional<FieldCondition> schedule3ActivitiesSelected;
    List<FieldCondition> primaryConditions;
    List<FieldCondition> validationConditions;
    Optional<EsaPointsCondition> validationPointsCondition;

    EsaAllowedOrRefusedCondition(AllowedOrRefusedCondition allowedOrRefusedCondition, YesNoFieldCondition supportGroupOnlyCondition, Optional<EsaPointsCondition> primaryPointsCondition, Optional<StringListPredicate> schedule3ActivitiesSelected,
        FieldCondition...validationConditions) {
        this(allowedOrRefusedCondition, supportGroupOnlyCondition, primaryPointsCondition, schedule3ActivitiesSelected, isAnyPoints(), validationConditions);
    }

    EsaAllowedOrRefusedCondition(AllowedOrRefusedCondition allowedOrRefusedCondition, YesNoFieldCondition supportGroupOnlyCondition, Optional<EsaPointsCondition> primaryPointsCondition, Optional<StringListPredicate> schedule3ActivitiesSelected, Optional<EsaPointsCondition> validationPointsCondition,
        FieldCondition...validationConditions) {
        if (schedule3ActivitiesSelected.isPresent()) {
            this.schedule3ActivitiesSelected = Optional.of(isSchedule3ActivitiesAnswer(schedule3ActivitiesSelected.get()));
        } else {
            this.schedule3ActivitiesSelected = Optional.empty();
        }
        this.primaryPointsCondition = primaryPointsCondition;
        this.primaryConditions = new ArrayList<>();
        primaryConditions.add(allowedOrRefusedCondition);
        primaryConditions.add(supportGroupOnlyCondition);
        if (this.schedule3ActivitiesSelected.isPresent()) {
            primaryConditions.add(this.schedule3ActivitiesSelected.get());
        }
        this.validationPointsCondition = validationPointsCondition;
        this.validationConditions = Arrays.asList(validationConditions);
    }

    static YesNoFieldCondition isRegulation29(Predicate<YesNo> predicate) {
        return new YesNoFieldCondition("Regulation 29", predicate,
                SscsCaseData::getDoesRegulation29Apply);
    }

    static YesNoFieldCondition isSupportGroupOnly(Predicate<YesNo> predicate) {
        return new YesNoFieldCondition("Support Group Only Appeal", predicate,
            s -> s.isSupportGroupOnlyAppeal() ? YesNo.YES : YesNo.NO);
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
            SscsCaseData::getRegulation35Selection);
    }

    static FieldCondition isSchedule3ActivitiesAnswer(StringListPredicate predicate) {
        return new StringListFieldCondition("Schedule 3 Activities", predicate,
            SscsCaseData::getSchedule3Selections);
    }

    @Override
    public boolean isApplicable(DecisionNoticeQuestionService questionService, SscsCaseData caseData) {
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
            "No points condition found for " + caseData.getDoesRegulation29Apply() + ":" + caseData.getSchedule3Selections() + ":" + caseData.getRegulation35Selection());
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
        return sscsCaseData -> CollectionUtils.collate(emptyIfNull(sscsCaseData.getEsaWriteFinalDecisionPhysicalDisabilitiesQuestion()),
            emptyIfNull(sscsCaseData.getEsaWriteFinalDecisionMentalAssessmentQuestion()));
    }
}
