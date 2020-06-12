package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

/**
 * Enum to encapsulate binding between an activity question key, and the corresponding getter method on SscsCaseData that yields that answer to that question.
 */
public enum ActivityQuestion {

    PREPARING_FOOD("preparingFood", "Preparing food", ActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionPreparingFoodQuestion),
    TAKING_NUTRITION("takingNutrition", "Taking nutrition",  ActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionTakingNutritionQuestion),
    MANAGING_THERAPY("managingTherapy", "Managing therapy or monitoring a health condition", ActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionManagingTherapyQuestion),
    WASHING_AND_BATHING("washingAndBathing", "Washing and bathing", ActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionWashAndBatheQuestion),
    MANAGING_TOILET_NEEDS("managingToiletNeeds", "Managing toilet needs or incontinence", ActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionManagingToiletNeedsQuestion),
    DRESSING_AND_UNDRESSING("dressingAndUndressing", "Dressing and undressing", ActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionDressingAndUndressingQuestion),
    COMMUNICATING("communicating", "Communicating", ActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionCommunicatingQuestion),
    READING_AND_UNDERSTANDING("readingUnderstanding", "Reading and understanding signs, symbols and words", ActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionReadingUnderstandingQuestion),
    ENGAGING_WITH_OTHERS("engagingWithOthers", "Engaging with other people face to face", ActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionEngagingWithOthersQuestion),
    MAKING_BUDGETING_DECISIONS("budgetingDecisions", "Making budgeting decisions", ActivityType.DAILY_LIVING, SscsCaseData::getPipWriteFinalDecisionBudgetingDecisionsQuestion),
    PLANNING_AND_FOLLOWING_JOURNEYS("planningAndFollowing", "Planning and following journeys", ActivityType.MOBILITY, SscsCaseData::getPipWriteFinalDecisionPlanningAndFollowingQuestion),
    MOVING_AROUND("movingAround", "Moving around", ActivityType.MOBILITY, SscsCaseData::getPipWriteFinalDecisionMovingAroundQuestion);

    final String key;
    final String value;
    final ActivityType activityType;
    final Function<SscsCaseData, String> answerExtractor;

    ActivityQuestion(String key, String value, ActivityType activityType, Function<SscsCaseData, String> answerExtractor) {
        this.key = key;
        this.answerExtractor = answerExtractor;
        this.activityType = activityType;
        this.value = value;
    }

    public static ActivityQuestion getByKey(String key) {
        for (ActivityQuestion mapping : ActivityQuestion.values()) {
            if (mapping.key.equals(key)) {
                return mapping;
            }
        }
        throw new IllegalArgumentException("Unknown ActivityQuestion for question key:" + key);
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public Function<SscsCaseData, String> getAnswerExtractor() {
        return answerExtractor;
    }
}
