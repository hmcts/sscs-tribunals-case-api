package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

/**
 * Enum to encapsulate binding between an activity question key, and the corresponding getter method on SscsCaseData that yields that answer to that question.
 */
public enum ActivityQuestion {

    PREPARING_FOOD("preparingFood", SscsCaseData::getPipWriteFinalDecisionPreparingFoodQuestion),
    TAKING_NUTRITION("takingNutrition", SscsCaseData::getPipWriteFinalDecisionTakingNutritionQuestion),
    MANAGING_THERAPY("managingTherapy", SscsCaseData::getPipWriteFinalDecisionManagingTherapyQuestion),
    WASHING_AND_BATHING("washingAndBathing", SscsCaseData::getPipWriteFinalDecisionWashAndBatheQuestion),
    MANAGING_TOILET_NEEDS("managingToiletNeeds", SscsCaseData::getPipWriteFinalDecisionManagingToiletNeedsQuestion),
    DRESSING_AND_UNDRESSING("dressingAndUndressing", SscsCaseData::getPipWriteFinalDecisionDressingAndUndressingQuestion),
    COMMUNICATING("communicating", SscsCaseData::getPipWriteFinalDecisionCommunicatingQuestion),
    READING_AND_UNDERSTANDING("readingUnderstanding", SscsCaseData::getPipWriteFinalDecisionReadingUnderstandingQuestion),
    ENGAGING_WITH_OTHERS("engagingWithOthers", SscsCaseData::getPipWriteFinalDecisionEngagingWithOthersQuestion),
    MAKING_BUDGETING_DECISIONS("budgetingDecisions", SscsCaseData::getPipWriteFinalDecisionBudgetingDecisionsQuestion),
    PLANNING_AND_FOLLOWING_JOURNEYS("planningAndFollowing", SscsCaseData::getPipWriteFinalDecisionPlanningAndFollowingQuestion),
    MOVING_AROUND("movingAround", SscsCaseData::getPipWriteFinalDecisionMovingAroundQuestion);

    final String key;
    final Function<SscsCaseData, String> answerExtractor;

    ActivityQuestion(String key, Function<SscsCaseData, String> answerExtractor) {
        this.key = key;
        this.answerExtractor = answerExtractor;
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
