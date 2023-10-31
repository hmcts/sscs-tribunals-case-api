package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.ccd.domain.YesNo.isYes;
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
import org.apache.commons.collections4.CollectionUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.YesNo;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AllowedOrRefusedCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AllowedOrRefusedPredicate;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardTypeCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardTypePredicate;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.FieldCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.PointsCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.YesNoFieldCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip.scenarios.PipScenario;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;

/**
 * Encapsulates the conditions satisfied by valid combinations of allowed/refused and other attributes of the Decision Notice journey - to be used on Outcome validation (eg. on submission), but not on
 * preview.
 */
public enum PipAllowedOrRefusedCondition implements PointsCondition<PipAllowedOrRefusedCondition> {

    REFUSED_NOT_CONSIDERED_NOT_CONSIDERED(
        isAllowedOrRefused(REFUSED),
        isDescriptorFlow(FALSE, false),
        isDailyLivingComparedToDwp(NOT_CONSIDERED),
        isMobilityComparedToDwp(NOT_CONSIDERED),
        isDailyLivingAward(AwardTypePredicate.NOT_CONSIDERED),
        isMobilityAward(AwardTypePredicate.NOT_CONSIDERED)),
    ALLOWED_NOT_CONSIDERED_NOT_CONSIDERED(
        isAllowedOrRefused(ALLOWED),
        isDescriptorFlow(FALSE, false),
        isDailyLivingComparedToDwp(NOT_CONSIDERED),
        isMobilityComparedToDwp(NOT_CONSIDERED),
        isDailyLivingAward(AwardTypePredicate.NOT_CONSIDERED),
        isMobilityAward(AwardTypePredicate.NOT_CONSIDERED)),
    REFUSED_NOT_CONSIDERED_LOWER(
        isDescriptorFlow(TRUE, false),
        isDailyLivingComparedToDwp(NOT_CONSIDERED),
        isMobilityComparedToDwp(LOWER),
        isDailyLivingAward(AwardTypePredicate.NOT_CONSIDERED),
        isMobilityAward(AwardTypePredicate.CONSIDERED)),
    REFUSED_NOT_CONSIDERED_SAME(
        isDescriptorFlow(TRUE, false),
        isDailyLivingComparedToDwp(NOT_CONSIDERED),
        isMobilityComparedToDwp(SAME),
        isDailyLivingAward(AwardTypePredicate.NOT_CONSIDERED),
        isMobilityAward(AwardTypePredicate.CONSIDERED)),
    ALLOWED_NOT_CONSIDERED_HIGHER(
        isDescriptorFlow(TRUE, false),
        isDailyLivingComparedToDwp(NOT_CONSIDERED),
        isMobilityComparedToDwp(HIGHER),
        isDailyLivingAward(AwardTypePredicate.NOT_CONSIDERED),
        isMobilityAward(AwardTypePredicate.CONSIDERED)),
    REFUSED_LOWER_NOT_CONSIDERED(
        isDescriptorFlow(TRUE, false),
        isDailyLivingComparedToDwp(LOWER),
        isMobilityComparedToDwp(NOT_CONSIDERED),
        isDailyLivingAward(AwardTypePredicate.CONSIDERED),
        isMobilityAward(AwardTypePredicate.NOT_CONSIDERED)),
    REFUSED_LOWER_LOWER(
        isDescriptorFlow(TRUE, false),
        isDailyLivingComparedToDwp(LOWER),
        isMobilityComparedToDwp(LOWER),
        isDailyLivingAward(AwardTypePredicate.CONSIDERED),
        isMobilityAward(AwardTypePredicate.CONSIDERED)),
    REFUSED_LOWER_SAME(
        isDescriptorFlow(TRUE, false),
        isDailyLivingComparedToDwp(LOWER),
        isMobilityComparedToDwp(SAME),
        isDailyLivingAward(AwardTypePredicate.CONSIDERED),
        isMobilityAward(AwardTypePredicate.CONSIDERED)),
    ALLOWED_LOWER_HIGHER(
        isDescriptorFlow(TRUE, false),
        isDailyLivingComparedToDwp(LOWER),
        isMobilityComparedToDwp(HIGHER),
        isDailyLivingAward(AwardTypePredicate.CONSIDERED),
        isMobilityAward(AwardTypePredicate.CONSIDERED)),
    REFUSED_SAME_NOT_CONSIDERED(
        isDescriptorFlow(TRUE, false),
        isDailyLivingComparedToDwp(SAME),
        isMobilityComparedToDwp(NOT_CONSIDERED),
        isDailyLivingAward(AwardTypePredicate.CONSIDERED),
        isMobilityAward(AwardTypePredicate.NOT_CONSIDERED)),
    REFUSED_SAME_LOWER(
        isDescriptorFlow(TRUE, false),
        isDailyLivingComparedToDwp(SAME),
        isMobilityComparedToDwp(LOWER),
        isDailyLivingAward(AwardTypePredicate.CONSIDERED),
        isMobilityAward(AwardTypePredicate.CONSIDERED)),
    REFUSED_SAME_SAME(
        isDescriptorFlow(TRUE, false),
        isDailyLivingComparedToDwp(SAME),
        isMobilityComparedToDwp(SAME),
        isDailyLivingAward(AwardTypePredicate.CONSIDERED),
        isMobilityAward(AwardTypePredicate.CONSIDERED)),
    ALLOWED_SAME_HIGHER(
        isDescriptorFlow(TRUE, false),
        isDailyLivingComparedToDwp(SAME),
        isMobilityComparedToDwp(HIGHER),
        isDailyLivingAward(AwardTypePredicate.CONSIDERED),
        isMobilityAward(AwardTypePredicate.CONSIDERED)),
    ALLOWED_HIGHER_NOT_CONSIDERED(
        isDescriptorFlow(TRUE, false),
        isDailyLivingComparedToDwp(HIGHER),
        isMobilityComparedToDwp(NOT_CONSIDERED),
        isDailyLivingAward(AwardTypePredicate.CONSIDERED),
        isMobilityAward(AwardTypePredicate.NOT_CONSIDERED)),
    ALLOWED_HIGHER_LOWER(
        isDescriptorFlow(TRUE, false),
        isDailyLivingComparedToDwp(HIGHER),
        isMobilityComparedToDwp(LOWER),
        isDailyLivingAward(AwardTypePredicate.CONSIDERED),
        isMobilityAward(AwardTypePredicate.CONSIDERED)),
    ALLOWED_HIGHER_SAME(
        isDescriptorFlow(TRUE, false),
        isDailyLivingComparedToDwp(HIGHER),
        isMobilityComparedToDwp(SAME),
        isDailyLivingAward(AwardTypePredicate.CONSIDERED),
        isMobilityAward(AwardTypePredicate.CONSIDERED)),
    ALLOWED_HIGHER_HIGHER(
        isDescriptorFlow(TRUE, false),
        isDailyLivingComparedToDwp(HIGHER),
        isMobilityComparedToDwp(HIGHER),
        isDailyLivingAward(AwardTypePredicate.CONSIDERED),
        isMobilityAward(AwardTypePredicate.CONSIDERED));

    List<FieldCondition> primaryConditions;
    boolean isDailyLivingConsidered;
    boolean isMobilityConsidered;

    PipAllowedOrRefusedCondition(Optional<AllowedOrRefusedCondition> allowedOrRefusedCondition, YesNoFieldCondition descriptorFlowCondition,
        DailyLivingComparedToDwpCondition dailyLivingComparedToDwpCondition,
        MobilityComparedToDwpCondition mobilityLivingComparedToDwpCondition, AwardTypeCondition dailyLivingCondition, AwardTypeCondition mobilityCondition) {
        this.isDailyLivingConsidered = dailyLivingCondition.getAwardTypePredicate() == AwardTypePredicate.CONSIDERED;
        this.isMobilityConsidered =  mobilityCondition.getAwardTypePredicate() == AwardTypePredicate.CONSIDERED;;
        this.primaryConditions = new ArrayList<>();
        this.primaryConditions.add(descriptorFlowCondition);
        if (allowedOrRefusedCondition.isPresent()) {
            this.primaryConditions.add(allowedOrRefusedCondition.get());
        }
        if (isDailyLivingConsidered || isMobilityConsidered) {
            this.primaryConditions.add(dailyLivingCondition);
            this.primaryConditions.add(mobilityCondition);
            if (isDailyLivingConsidered) {
                this.primaryConditions.add(dailyLivingComparedToDwpCondition);
            }
            if (isMobilityConsidered) {
                this.primaryConditions.add(mobilityLivingComparedToDwpCondition);
            }
        }
    }

    PipAllowedOrRefusedCondition(YesNoFieldCondition descriptorFlowCondition, DailyLivingComparedToDwpCondition dailyLivingComparedToDwpCondition,
        MobilityComparedToDwpCondition mobilityLivingComparedToDwpCondition, AwardTypeCondition dailyLivingCondition, AwardTypeCondition mobilityCondition) {
        this.isDailyLivingConsidered = dailyLivingCondition.getAwardTypePredicate() == AwardTypePredicate.CONSIDERED;
        this.isMobilityConsidered =  mobilityCondition.getAwardTypePredicate() == AwardTypePredicate.CONSIDERED;;
        this.primaryConditions = new ArrayList<>();
        this.primaryConditions.add(descriptorFlowCondition);
        if (isDailyLivingConsidered || isMobilityConsidered) {
            this.primaryConditions.add(dailyLivingCondition);
            this.primaryConditions.add(mobilityCondition);
            if (isDailyLivingConsidered) {
                this.primaryConditions.add(dailyLivingComparedToDwpCondition);
            }
            if (isMobilityConsidered) {
                this.primaryConditions.add(mobilityLivingComparedToDwpCondition);
            }
        }
    }

    public static Optional<PipAllowedOrRefusedCondition> getPassingAllowedOrRefusedCondition(DecisionNoticeQuestionService questionService,
        SscsCaseData caseData) {
        return Optional.of(PipAllowedOrRefusedCondition.getTheSinglePassingPointsConditionForSubmittedActivitiesAndPoints(questionService, caseData));
    }

    static YesNoFieldCondition isDescriptorFlow(Predicate<YesNo> predicate, boolean displayIsSatisfiedMessage) {
        return new YesNoFieldCondition("Descriptor Flow", predicate,
            s -> isYes(s.getSscsFinalDecisionCaseData().getWriteFinalDecisionIsDescriptorFlow()) ? YesNo.YES : YesNo.NO, displayIsSatisfiedMessage);
    }

    static Optional<AllowedOrRefusedCondition> isAllowedOrRefused(AllowedOrRefusedPredicate predicate) {
        return Optional.of(new AllowedOrRefusedCondition(predicate));
    }

    static AwardTypeCondition isDailyLivingAward(AwardTypePredicate awardTypePredicate) {
        return new AwardTypeCondition(awardTypePredicate, c -> c.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion(), PipActivityType.DAILY_LIVING);
    }

    static AwardTypeCondition isMobilityAward(AwardTypePredicate awardTypePredicate) {
        return new AwardTypeCondition(awardTypePredicate, c -> c.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion(), PipActivityType.MOBILITY);
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
            "No allowed/refused condition found for " + caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionIsDescriptorFlow()
                + ":" + caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionAllowedOrRefused()
                + ":" + caseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion() + ":"
                + caseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion() + ":"
                + caseData.getSscsPipCaseData()
                .getPipWriteFinalDecisionComparedToDwpDailyLivingQuestion() + ":"
                + caseData.getSscsPipCaseData().getPipWriteFinalDecisionComparedToDwpMobilityQuestion());
    }

    public static Function<SscsCaseData, List<String>> getAllAnswersExtractor() {
        return sscsCaseData -> CollectionUtils.collate(emptyIfNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionPhysicalDisabilitiesQuestion()),
            emptyIfNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionMentalAssessmentQuestion()));
    }

    public PipScenario getPipScenario(SscsCaseData caseData) {
        if (REFUSED_NOT_CONSIDERED_NOT_CONSIDERED == this || ALLOWED_NOT_CONSIDERED_NOT_CONSIDERED == this) {
            return PipScenario.SCENARIO_NON_DESCRIPTOR;
        } else if (isDailyLivingConsidered && isMobilityConsidered) {
            if ("noAward".equals(caseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion()) && "noAward".equals(caseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion())) {
                return PipScenario.SCENARIO_NO_AWARD_NO_AWARD;
            } else if ("noAward".equals(caseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion())) {
                return PipScenario.SCENARIO_NO_AWARD_AWARD;
            } else if ("noAward".equals(caseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion())) {
                return PipScenario.SCENARIO_AWARD_NO_AWARD;
            } else {
                return PipScenario.SCENARIO_AWARD_AWARD;
            }
        } else if (isDailyLivingConsidered) {
            if ("noAward".equals(caseData.getSscsPipCaseData().getPipWriteFinalDecisionDailyLivingQuestion())) {
                return PipScenario.SCENARIO_NO_AWARD_NOT_CONSIDERED;
            } else {
                return PipScenario.SCENARIO_AWARD_NOT_CONSIDERED;
            }
        } else if (isMobilityConsidered) {
            if ("noAward".equals(caseData.getSscsPipCaseData().getPipWriteFinalDecisionMobilityQuestion())) {
                return PipScenario.SCENARIO_NOT_CONSIDERED_NO_AWARD;
            } else {
                return PipScenario.SCENARIO_NOT_CONSIDERED_AWARD;
            }
        } else {
            throw new IllegalStateException("Should not happen");
        }
    }

    @Override
    public boolean isApplicable(DecisionNoticeQuestionService questionService, SscsCaseData caseData) {
        if (isYes(caseData.getSscsFinalDecisionCaseData().getWriteFinalDecisionGenerateNotice())) {
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
        return Optional.empty();
    }
}

