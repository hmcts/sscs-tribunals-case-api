package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.pip;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.IntPredicate;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityAnswer;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.AwardType;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.PointsCondition;
import uk.gov.hmcts.reform.sscs.service.DecisionNoticeQuestionService;

/**
 * Enum encapsulating the attributes of a points-related condition on SscsCaseData. Each condition specifies the type of award the condition applies for, the activity type it applies to, along with
 * points criteria and an error message to display if the points criteria are not met.
 */
public enum PipPointsCondition implements PointsCondition<PipPointsCondition> {

    DAILY_LIVING_STANDARD(AwardType.STANDARD_RATE,
        PipActivityType.DAILY_LIVING,
        points -> points >= 8 && points <= 11),
    DAILY_LIVING_ENHANCED(AwardType.ENHANCED_RATE,
        PipActivityType.DAILY_LIVING,
        points -> points >= 12),
    DAILY_LIVING_NO_AWARD(AwardType.NO_AWARD,
        PipActivityType.DAILY_LIVING,
        points -> points <= 7),
    MOBILITY_STANDARD(AwardType.STANDARD_RATE,
        PipActivityType.MOBILITY,
        points -> points >= 8 && points <= 11),
    MOBILITY_ENHANCED(AwardType.ENHANCED_RATE,
        PipActivityType.MOBILITY,
        points -> points >= 12),
    MOBILITY_NO_AWARD(AwardType.NO_AWARD,
        PipActivityType.MOBILITY,
        points -> points <= 7);

    final AwardType awardType;
    final String errorMessage;
    final PipActivityType activityType;
    final IntPredicate pointsRequirementCondition;

    PipPointsCondition(AwardType awardType, PipActivityType activityType,
        IntPredicate pointsRequirementCondition) {
        this.awardType = awardType;
        this.pointsRequirementCondition = pointsRequirementCondition;
        this.activityType = activityType;
        this.errorMessage = getStandardErrorMessage(awardType, activityType);
    }

    public boolean isApplicable(DecisionNoticeQuestionService questionService, SscsCaseData sscsCaseData) {
        return awardType.getKey().equals(activityType.getAwardTypeExtractor().apply(sscsCaseData));
    }

    public PipActivityType getActivityType() {
        return activityType;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public Class<PipPointsCondition> getEnumClass() {
        return PipPointsCondition.class;
    }

    @Override
    public Function<SscsCaseData, List<String>> getAnswersExtractor() {
        return activityType.getAnswersExtractor();
    }

    public IntPredicate getPointsRequirementCondition() {
        return pointsRequirementCondition;
    }

    protected static String getStandardErrorMessage(AwardType awardType, ActivityType activityType) {
        final String awardDescription;
        if (awardType == AwardType.NO_AWARD) {
            awardDescription = "No Award";
        } else if (awardType == AwardType.STANDARD_RATE) {
            awardDescription = "a standard rate award";
        } else if (awardType == AwardType.ENHANCED_RATE) {
            awardDescription = "an enhanced rate award";
        } else {
            throw new IllegalArgumentException("Unable to construct a "
                + "standard PointsCondition error message for award type:" + awardType);
        }
        return "You have previously selected " + awardDescription + " for " + activityType.getName()
            + ". The points awarded don't match. Please review your previous selection.";
    }

    /**
     * Given a points condition, and an SscsCaseData instance, obtain an error message for that condition if the condition has failed to be satified, or an empty optional if the condition is met.
     *
     * @param questionService The question service to use
     * @param sscsCaseData The SscsCaseData to evaluate against the condition.
     * @return An optional error message if the condition has failed to be satified, or an empty optional if the condition is met.
     */
    public Optional<String> getOptionalErrorMessage(DecisionNoticeQuestionService questionService, SscsCaseData sscsCaseData) {

        Collection<String> collection = emptyIfNull(getAnswersExtractor().apply(sscsCaseData));

        if (collection.isEmpty()) {
            return Optional.empty();
        }
        int totalPoints = collection.stream().map(answerText -> questionService.getAnswerForActivityQuestionKey(sscsCaseData,
            answerText)).filter(Optional::isPresent).map(Optional::get).mapToInt(ActivityAnswer::getActivityAnswerPoints).sum();

        return getPointsRequirementCondition().test(totalPoints) ? Optional.empty() :
            Optional.of(getErrorMessage());

    }
}
