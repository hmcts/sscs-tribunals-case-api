package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

/**
 * Enum to encapsulate binding between an activity question key, and the corresponding getter method on SscsCaseData that yields that answer to that question.
 */
public enum ActivityQuestion {

    PREPARING_FOOD("preparingFood", ActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionPreparingFoodQuestion),
    TAKING_NUTRITION("takingNutrition", ActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionTakingNutritionQuestion),
    MANAGING_THERAPY("managingTherapy", ActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionManagingTherapyQuestion),
    WASHING_AND_BATHING("washingAndBathing", ActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionWashAndBatheQuestion),
    MANAGING_TOILET_NEEDS("managingToiletNeeds", ActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionManagingToiletNeedsQuestion),
    DRESSING_AND_UNDRESSING("dressingAndUndressing", ActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionDressingAndUndressingQuestion),
    COMMUNICATING("communicating", ActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionCommunicatingQuestion),
    READING_AND_UNDERSTANDING("readingUnderstanding", ActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionReadingUnderstandingQuestion),
    ENGAGING_WITH_OTHERS("engagingWithOthers", ActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionEngagingWithOthersQuestion),
    MAKING_BUDGETING_DECISIONS("budgetingDecisions", ActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionBudgetingDecisionsQuestion),
    PLANNING_AND_FOLLOWING_JOURNEYS("planningAndFollowing", ActivityType.MOBILITY, SscsCaseData::getPipWriteFinalDecisionPlanningAndFollowingQuestion),
    MOVING_AROUND("movingAround", ActivityType.MOBILITY, SscsCaseData::getPipWriteFinalDecisionMovingAroundQuestion);

    final String key;
    final ActivityType activityType;
    final Function<SscsCaseData, String> answerExtractor;

    ActivityQuestion(String key, ActivityType activityType, Function<SscsCaseData, String> answerExtractor) {
        this.key = key;
        this.answerExtractor = answerExtractor;
        this.activityType = activityType;
    }

    public static ActivityQuestion getByKey(String key) {
        for (ActivityQuestion mapping : ActivityQuestion.values()) {
            if (mapping.key.equals(key)) {
                return mapping;
            }
        }
        throw new IllegalArgumentException("Unknown ActivityQuestion for question key:" + key);
    }

    public Function<SscsCaseData, String> getAnswerExtractor() {
        return answerExtractor;
    }
}
