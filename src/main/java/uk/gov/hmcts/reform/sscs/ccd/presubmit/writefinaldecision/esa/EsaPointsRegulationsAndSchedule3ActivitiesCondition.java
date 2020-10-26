package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.YesNoPredicate.FALSE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.YesNoPredicate.SPECIFIED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.YesNoPredicate.TRUE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa.YesNoPredicate.UNSPECIFIED;

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
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;
import uk.gov.hmcts.reform.sscs.utility.StringUtils;

public enum EsaPointsRegulationsAndSchedule3ActivitiesCondition implements PointsCondition<EsaPointsRegulationsAndSchedule3ActivitiesCondition> {

    LOW_POINTS_REGULATION_29_UNSPECIFIED(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        isRegulation29(UNSPECIFIED), Optional.empty(), isRegulation29(SPECIFIED)),
    LOW_POINTS_REGULATION_29_DOES_NOT_APPLY(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        isRegulation29(FALSE), Optional.of(AwardType.NO_AWARD), isRegulation35(UNSPECIFIED), isSchedule3ActivitiesAnswer(StringListPredicate.UNSPECIFIED)),
    LOW_POINTS_REGULATION_29_DOES_APPLY_REGULATION_35_UNSPECIFIED(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isRegulation29(TRUE), isRegulation35(UNSPECIFIED)), Optional.of(AwardType.HIGHER_RATE),  isSchedule3ActivitiesAnswer(StringListPredicate.NOT_EMPTY)),
    LOW_POINTS_REGULATION_29_DOES_APPLY_REGULATION_35_DOES_NOT_APPLY(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isRegulation29(TRUE), isRegulation35(FALSE)), Optional.of(AwardType.LOWER_RATE), isSchedule3ActivitiesAnswer(StringListPredicate.EMPTY)),
    LOW_POINTS_REGULATION_29_DOES_APPLY_REGULATION_35_DOES_APPLY(EsaPointsCondition.POINTS_LESS_THAN_FIFTEEN,
        Arrays.asList(isRegulation29(TRUE), isRegulation35(TRUE)), Optional.of(AwardType.HIGHER_RATE), isSchedule3ActivitiesAnswer(StringListPredicate.EMPTY)),
    HIGH_POINTS_REGULATION_35_UNSPECIFIED(EsaPointsCondition.POINTS_GREATER_OR_EQUAL_TO_FIFTEEN,
        isRegulation35(UNSPECIFIED), Optional.of(AwardType.HIGHER_RATE), isRegulation29(UNSPECIFIED), isSchedule3ActivitiesAnswer(StringListPredicate.NOT_EMPTY)),
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

    EsaPointsRegulationsAndSchedule3ActivitiesCondition(EsaPointsCondition pointsCondition, List<YesNoFieldCondition> primaryConditions,
        Optional<AwardType> awardType, FieldCondition...validationConditions) {
        this.pointsCondition = pointsCondition;
        this.awardType = awardType;
        this.primaryConditions = primaryConditions;
        this.validationConditions = Arrays.asList(validationConditions);
    }

    static YesNoFieldCondition isRegulation29(YesNoPredicate predicate) {
        return new YesNoFieldCondition("Regulation 29", predicate,
            c -> c.getDoesRegulation29Apply());
    }

    static YesNoFieldCondition isRegulation35(YesNoPredicate predicate) {
        return new YesNoFieldCondition("Regulation 35", predicate,
            c -> c.getDoesRegulation35Apply());
    }

    private static List<String> getActivities(SscsCaseData caseData) {
        // FIXME Implement once activities supported
        return null;
    }

    static FieldCondition isSchedule3ActivitiesAnswer(StringListPredicate predicate) {
        return new StringListFieldCondition("Activities", predicate,
            c -> getActivities(c));
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

    @Override
    public Optional<String> getOptionalErrorMessage(DecisionNoticeQuestionService questionService, SscsCaseData sscsCaseData) {
        List<String> errorMessages =
            validationConditions.stream()
                .filter(c -> !(c instanceof StringListFieldCondition)) // FIXME Remove once activities supported
                .map(e -> e.getOptionalErrorMessage(sscsCaseData))
                .filter(Optional::isPresent)
                .map(o -> o.get())
                .collect(Collectors.toList());
        if (!errorMessages.isEmpty()) {
            return Optional.of("You have submitted " + StringUtils.getGramaticallyJoinedStrings(errorMessages)
               + ". The points awarded don't match. Please review your previous selection.");
        }
        return Optional.empty();
    }

    public static Function<SscsCaseData, List<String>> getAllAnswersExtractor() {
        return sscsCaseData -> CollectionUtils.collate(emptyIfNull(sscsCaseData.getEsaWriteFinalDecisionPhysicalDisabilitiesQuestion()),
            emptyIfNull(sscsCaseData.getEsaWriteFinalDecisionMentalAssessmentQuestion()));
    }
}
