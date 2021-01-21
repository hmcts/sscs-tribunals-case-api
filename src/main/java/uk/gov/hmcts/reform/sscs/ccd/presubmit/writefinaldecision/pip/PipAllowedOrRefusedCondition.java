package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AllowedOrRefusedPredicate.ALLOWED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AllowedOrRefusedPredicate.REFUSED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoPredicate.FALSE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoPredicate.TRUE;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.ComparedToDwpPredicate.HIGHER;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.ComparedToDwpPredicate.LOWER;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.ComparedToDwpPredicate.NOT_CONSIDERED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.ComparedToDwpPredicate.SAME;

import java.util.ArrayList;
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
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoFieldCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios.PipScenario;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;
import uk.gov.hmcts.reform.sscs.utility.StringUtils;

/**
 * Encapsulates the conditions satisfied by valid combinations of allowed/refused and other attributes of the Decision Notice journey - to be used on Outcome validation (eg. on submission), but not on
 * preview.
 */
public enum PipAllowedOrRefusedCondition implements PointsCondition<PipAllowedOrRefusedCondition> {

    REFUSED_NOT_CONSIDERED_NOT_CONSIDERED(
            isAllowedOrRefused(REFUSED),
            isDescriptorFlow(FALSE, false),
            isDailyLivingComparedToDwp(NOT_CONSIDERED),
            isMobilityComparedToDwp(NOT_CONSIDERED), false, false),
    ALLOWED_NOT_CONSIDERED_NOT_CONSIDERED(
            isAllowedOrRefused(ALLOWED),
            isDescriptorFlow(FALSE, false),
            isDailyLivingComparedToDwp(NOT_CONSIDERED),
            isMobilityComparedToDwp(NOT_CONSIDERED), false, false),
    REFUSED_NOT_CONSIDERED_LOWER(
            isAllowedOrRefused(REFUSED),
            isDescriptorFlow(TRUE, false),
            isDailyLivingComparedToDwp(NOT_CONSIDERED),
            isMobilityComparedToDwp(LOWER), false, true),
    REFUSED_NOT_CONSIDERED_SAME(
            isAllowedOrRefused(REFUSED),
            isDescriptorFlow(TRUE, false),
            isDailyLivingComparedToDwp(NOT_CONSIDERED),
            isMobilityComparedToDwp(SAME), false, true),
    ALLOWED_NOT_CONSIDERED_HIGHER(
            isAllowedOrRefused(ALLOWED),
            isDescriptorFlow(TRUE, false),
            isDailyLivingComparedToDwp(NOT_CONSIDERED),
            isMobilityComparedToDwp(HIGHER), false, true),
    REFUSED_LOWER_NOT_CONSIDERED(
            isAllowedOrRefused(REFUSED),
            isDescriptorFlow(TRUE, false),
            isDailyLivingComparedToDwp(LOWER),
            isMobilityComparedToDwp(NOT_CONSIDERED), true, false),
    REFUSED_LOWER_LOWER(
            isAllowedOrRefused(REFUSED),
            isDescriptorFlow(TRUE, false),
            isDailyLivingComparedToDwp(LOWER),
            isMobilityComparedToDwp(LOWER), true, true),
    REFUSED_LOWER_SAME(
            isAllowedOrRefused(REFUSED),
            isDescriptorFlow(TRUE, false),
            isDailyLivingComparedToDwp(LOWER),
            isMobilityComparedToDwp(SAME), true, true),
    ALLOWED_LOWER_HIGHER(
            isAllowedOrRefused(ALLOWED),
            isDescriptorFlow(TRUE, false),
            isDailyLivingComparedToDwp(LOWER),
            isMobilityComparedToDwp(HIGHER), true, true),
    REFUSED_SAME_NOT_CONSIDERED(
            isAllowedOrRefused(REFUSED),
            isDescriptorFlow(TRUE, false),
            isDailyLivingComparedToDwp(SAME),
            isMobilityComparedToDwp(NOT_CONSIDERED), true, false),
    REFUSED_SAME_LOWER(
            isAllowedOrRefused(REFUSED),
            isDescriptorFlow(TRUE, false),
            isDailyLivingComparedToDwp(SAME),
            isMobilityComparedToDwp(LOWER), true, true),
    REFUSED_SAME_SAME(
            isAllowedOrRefused(REFUSED),
            isDescriptorFlow(TRUE, false),
            isDailyLivingComparedToDwp(SAME),
            isMobilityComparedToDwp(SAME), true, true),
    ALLOWED_SAME_HIGHER(
            isAllowedOrRefused(ALLOWED),
            isDescriptorFlow(TRUE, false),
            isDailyLivingComparedToDwp(SAME),
            isMobilityComparedToDwp(HIGHER), true, true),
    ALLOWED_HIGHER_NOT_CONSIDERED(
            isAllowedOrRefused(ALLOWED),
            isDescriptorFlow(TRUE, false),
            isDailyLivingComparedToDwp(HIGHER),
            isMobilityComparedToDwp(NOT_CONSIDERED), true, false),
    ALLOWED_HIGHER_LOWER(
            isAllowedOrRefused(ALLOWED),
            isDescriptorFlow(TRUE, false),
            isDailyLivingComparedToDwp(HIGHER),
            isMobilityComparedToDwp(LOWER), true, true),
    ALLOWED_HIGHER_SAME(
            isAllowedOrRefused(ALLOWED),
            isDescriptorFlow(TRUE, false),
            isDailyLivingComparedToDwp(HIGHER),
            isMobilityComparedToDwp(SAME), true, true),
    ALLOWED_HIGHER_HIGHER(
            isAllowedOrRefused(ALLOWED),
            isDescriptorFlow(TRUE, false),
            isDailyLivingComparedToDwp(HIGHER),
            isMobilityComparedToDwp(HIGHER), true, true);

    List<FieldCondition> primaryConditions;
    List<FieldCondition> validationConditions;
    boolean isDailyLivingConsidered;
    boolean isMobilityConsidered;

    PipAllowedOrRefusedCondition(AllowedOrRefusedCondition allowedOrRefusedCondition, YesNoFieldCondition descriptorFlowCondition, DailyLivingComparedToDwpCondition dailyLivingComparedToDwpCondition,
                                 MobilityComparedToDwpCondition mobilityLivingComparedToDwpCondition, boolean isDailyLivingConsidered, boolean isMobilityConsidered) {
        this.primaryConditions = new ArrayList<>();
        this.validationConditions = new ArrayList<>();
        this.primaryConditions.add(allowedOrRefusedCondition);
        this.primaryConditions.add(descriptorFlowCondition);
        this.primaryConditions.add(dailyLivingComparedToDwpCondition);
        this.primaryConditions.add(mobilityLivingComparedToDwpCondition);
        this.isDailyLivingConsidered = isDailyLivingConsidered;
        this.isMobilityConsidered = isMobilityConsidered;

    }


    public PipScenario getPipScenario(SscsCaseData caseData) {

        if (REFUSED_NOT_CONSIDERED_NOT_CONSIDERED == this || ALLOWED_NOT_CONSIDERED_NOT_CONSIDERED == this) {
            return PipScenario.SCENARIO_NON_DESCRIPTOR;
        } else {
            if ("noAward".equals(caseData.getPipWriteFinalDecisionDailyLivingQuestion()) && "noAward".equals(caseData.getPipWriteFinalDecisionMobilityQuestion())) {
                return PipScenario.SCENARIO_NO_AWARD_NO_AWARD;
            } else if ("notConsidered".equals(caseData.getPipWriteFinalDecisionDailyLivingQuestion()) && "noAward".equals(caseData.getPipWriteFinalDecisionMobilityQuestion())) {
                return PipScenario.SCENARIO_NOT_CONSIDERED_NO_AWARD;
            } else if ("notConsidered".equals(caseData.getPipWriteFinalDecisionDailyLivingQuestion()) && (!"noAward".equals(caseData.getPipWriteFinalDecisionMobilityQuestion()) && !"notConsidered".equals(caseData.getPipWriteFinalDecisionMobilityQuestion()))) {
                return PipScenario.SCENARIO_NOT_CONSIDERED_AWARD;
            } else if ("noAward".equals(caseData.getPipWriteFinalDecisionDailyLivingQuestion()) && !"notConsidered".equals(caseData.getPipWriteFinalDecisionMobilityQuestion())) {
                return PipScenario.SCENARIO_NO_AWARD_AWARD;
            } else if ("noAward".equals(caseData.getPipWriteFinalDecisionDailyLivingQuestion()) && "notConsidered".equals(caseData.getPipWriteFinalDecisionMobilityQuestion())) {
                return PipScenario.SCENARIO_NO_AWARD_NOT_CONSIDERED;
            } else if ((!"noAward".equals(caseData.getPipWriteFinalDecisionDailyLivingQuestion()) && !"notConsidered".equals(caseData.getPipWriteFinalDecisionDailyLivingQuestion())) && "notConsidered".equals(caseData.getPipWriteFinalDecisionMobilityQuestion())) {
                return PipScenario.SCENARIO_AWARD_NOT_CONSIDERED;
            } else if ((!"noAward".equals(caseData.getPipWriteFinalDecisionDailyLivingQuestion()) && !"notConsidered".equals(caseData.getPipWriteFinalDecisionDailyLivingQuestion())) && "noAward".equals(caseData.getPipWriteFinalDecisionMobilityQuestion())) {
                return PipScenario.SCENARIO_AWARD_NO_AWARD;
            } else if ((!"noAward".equals(caseData.getPipWriteFinalDecisionDailyLivingQuestion()) && !"notConsidered".equals(caseData.getPipWriteFinalDecisionDailyLivingQuestion())) && (!"noAward".equals(caseData.getPipWriteFinalDecisionMobilityQuestion()) && !"notConsidered".equals(caseData.getPipWriteFinalDecisionMobilityQuestion()))) {
                return PipScenario.SCENARIO_AWARD_AWARD;
            } else {
                return null;
            }
            /*
            if ("noAward".equals(caseData.getPipWriteFinalDecisionDailyLivingQuestion()) && "noAward".equals(caseData.getPipWriteFinalDecisionMobilityQuestion())) {
                return PipScenario.SCENARIO_NO_AWARD_NO_AWARD;
            } else if ("noAward".equals(caseData.getPipWriteFinalDecisionDailyLivingQuestion())) {
                return PipScenario.SCENARIO_NO_AWARD_AWARD;
            } else if ("noAward".equals(caseData.getPipWriteFinalDecisionMobilityQuestion())) {
                return PipScenario.SCENARIO_AWARD_NO_AWARD;
            } else {
                return PipScenario.SCENARIO_AWARD_AWARD;
            }
        } else if (isDailyLivingConsidered) {
            if ("noAward".equals(caseData.getPipWriteFinalDecisionDailyLivingQuestion())) {
                return PipScenario.SCENARIO_NO_AWARD_NO_AWARD;
            } else {
                return PipScenario.SCENARIO_AWARD_NOT_CONSIDERED;
            }
        } else if (isMobilityConsidered) {
            if ("noAward".equals(caseData.getPipWriteFinalDecisionMobilityQuestion())) {
                return PipScenario.SCENARIO_NOT_CONSIDERED_NO_AWARD;
            } else {
                return PipScenario.SCENARIO_NOT_CONSIDERED_AWARD;
            }
            */
        }
    }

    public static Optional<PipAllowedOrRefusedCondition> getPassingAllowedOrRefusedCondition(DecisionNoticeQuestionService questionService,
                                                                                             SscsCaseData caseData) {

        PipAllowedOrRefusedCondition condition
                = getTheSinglePassingPointsConditionForSubmittedActivitiesAndPoints(questionService, caseData);

        if (condition.getOptionalErrorMessage(questionService, caseData).isEmpty()) {
            return Optional.of(PipAllowedOrRefusedCondition.getTheSinglePassingPointsConditionForSubmittedActivitiesAndPoints(questionService, caseData));
        } else {
            return Optional.empty();
        }
    }

    static Optional<PipPointsCondition> isPoints(PipPointsCondition pointsCondition) {
        return Optional.of(pointsCondition);
    }

    static YesNoFieldCondition isDescriptorFlow(Predicate<YesNo> predicate, boolean displayIsSatisfiedMessage) {
        return new YesNoFieldCondition("Descriptor Flow", predicate,
                s -> "Yes".equalsIgnoreCase(s.getWriteFinalDecisionIsDescriptorFlow()) ? YesNo.YES : YesNo.NO, displayIsSatisfiedMessage);
    }

    static AllowedOrRefusedCondition isAllowedOrRefused(AllowedOrRefusedPredicate predicate) {
        return new AllowedOrRefusedCondition(predicate);
    }

    static DailyLivingComparedToDwpCondition isDailyLivingComparedToDwp(ComparedToDwpPredicate predicate) {
        return new DailyLivingComparedToDwpCondition(predicate);
    }

    static MobilityComparedToDwpCondition isMobilityComparedToDwp(ComparedToDwpPredicate predicate) {
        return new MobilityComparedToDwpCondition(predicate);
    }

    protected static PipAllowedOrRefusedCondition getTheSinglePassingPointsConditionForSubmittedActivitiesAndPoints(DecisionNoticeQuestionService questionService,
                                                                                                                    SscsCaseData caseData) {

        for (PipAllowedOrRefusedCondition pipPointsAndActivitiesCondition : PipAllowedOrRefusedCondition.values()) {

            if (pipPointsAndActivitiesCondition.isApplicable(questionService, caseData) && pipPointsAndActivitiesCondition.getOptionalErrorMessage(questionService, caseData).isEmpty()) {
                return pipPointsAndActivitiesCondition;
            }
        }
        throw new IllegalStateException(
                "No allowed/refused condition found for " + caseData.getWriteFinalDecisionIsDescriptorFlow() + ":" + caseData.getWriteFinalDecisionAllowedOrRefused() + ":" + caseData
                        .getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion() + ":" + caseData.getPipWriteFinalDecisionComparedToDwpMobilityQuestion());
    }

    public static Function<SscsCaseData, List<String>> getAllAnswersExtractor() {
        return sscsCaseData -> CollectionUtils.collate(emptyIfNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionPhysicalDisabilitiesQuestion()),
                emptyIfNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionMentalAssessmentQuestion()));
    }

    @Override
    public boolean isApplicable(DecisionNoticeQuestionService questionService, SscsCaseData caseData) {
        if ("Yes".equalsIgnoreCase(caseData.getWriteFinalDecisionGenerateNotice())) {
            return primaryConditions.stream().allMatch(c -> c.isSatisified(caseData));
        } else {
            return false;
        }
    }

    @Override
    public IntPredicate getPointsRequirementCondition() {
        return p -> true;
    }

    @Override
    public Class<PipAllowedOrRefusedCondition> getEnumClass() {
        return PipAllowedOrRefusedCondition.class;
    }

    @Override
    public Function<SscsCaseData, List<String>> getAnswersExtractor() {
        return getAllAnswersExtractor();
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
        /*
        if (primaryPointsCondition.isPresent()) {
            criteriaSatisfiedMessages.add(primaryPointsCondition.get().getIsSatisfiedMessage());
        }
         */
        criteriaSatisfiedMessages.addAll(primaryCriteriaSatisfiedMessages);

        List<String> validationMessages = new ArrayList<>();
        /*
        if (validationPointsCondition.isPresent()) {
            int points = questionService.getTotalPoints(sscsCaseData, getAnswersExtractor().apply(sscsCaseData));
            if (!validationPointsCondition.get().getPointsRequirementCondition().test(points)) {
                validationMessages.add(validationPointsCondition.get().getErrorMessage());
            }
        }
         */
        validationMessages.addAll(validationErrorMessages);

        if (!validationMessages.isEmpty()) {
            return Optional.of("You have " + StringUtils.getGramaticallyJoinedStrings(criteriaSatisfiedMessages)
                    + (criteriaSatisfiedMessages.isEmpty() ? "" : ", but have ") + StringUtils.getGramaticallyJoinedStrings(validationMessages)
                    + ". Please review your previous selection.");
        }
        return Optional.empty();
    }
}

