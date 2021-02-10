package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.dla;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AllowedOrRefusedPredicate.ALLOWED;
import static uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AllowedOrRefusedPredicate.REFUSED;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntPredicate;
import org.apache.commons.collections4.CollectionUtils;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AllowedOrRefusedCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AllowedOrRefusedPredicate;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.FieldCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.PointsCondition;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.dla.scenarios.DlaScenario;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;

/**
 * Encapsulates the conditions satisfied by valid combinations of allowed/refused and other attributes of the Decision Notice journey - to be used on Outcome validation (eg. on submission), but not on
 * preview.
 */
public enum DlaAllowedOrRefusedCondition implements PointsCondition<DlaAllowedOrRefusedCondition> {

    REFUSED_CONDITION(
        isAllowedOrRefused(REFUSED)),
    ALLOWED_CONDITION(
        isAllowedOrRefused(ALLOWED));

    List<FieldCondition> primaryConditions;

    DlaAllowedOrRefusedCondition(Optional<AllowedOrRefusedCondition> allowedOrRefusedCondition) {
        this.primaryConditions = new ArrayList<>();
        if (allowedOrRefusedCondition.isPresent()) {
            this.primaryConditions.add(allowedOrRefusedCondition.get());
        }
    }

    public static Optional<DlaAllowedOrRefusedCondition> getPassingAllowedOrRefusedCondition(DecisionNoticeQuestionService questionService,
        SscsCaseData caseData) {
        return Optional.of(DlaAllowedOrRefusedCondition.getTheSinglePassingPointsConditionForSubmittedActivitiesAndPoints(questionService, caseData));
    }

    static Optional<AllowedOrRefusedCondition> isAllowedOrRefused(AllowedOrRefusedPredicate predicate) {
        return Optional.of(new AllowedOrRefusedCondition(predicate));
    }

    protected static DlaAllowedOrRefusedCondition getTheSinglePassingPointsConditionForSubmittedActivitiesAndPoints(DecisionNoticeQuestionService questionService,
        SscsCaseData caseData) {

        for (DlaAllowedOrRefusedCondition dlaPointsAndActivitiesCondition : DlaAllowedOrRefusedCondition.values()) {

            if (dlaPointsAndActivitiesCondition.isApplicable(questionService, caseData)) {
                return dlaPointsAndActivitiesCondition;
            }
        }
        throw new IllegalStateException(
            "No allowed/refused condition found for " + caseData.getWriteFinalDecisionAllowedOrRefused());
    }

    public static Function<SscsCaseData, List<String>> getAllAnswersExtractor() {
        return sscsCaseData -> CollectionUtils.collate(emptyIfNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionPhysicalDisabilitiesQuestion()),
            emptyIfNull(sscsCaseData.getSscsEsaCaseData().getEsaWriteFinalDecisionMentalAssessmentQuestion()));
    }

    public DlaScenario getPipScenario() {
        return DlaScenario.SCENARIO_NON_DESCRIPTOR;
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
    public Class<DlaAllowedOrRefusedCondition> getEnumClass() {
        return DlaAllowedOrRefusedCondition.class;
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

