package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import java.util.List;
import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityType;

/**
 * Enum to encapsulate binding between an activity question key, and the corresponding getter method on SscsCaseData that yields that answer to that question.
 */
public enum UcSchedule3QuestionKey implements UcQuestionKey<Boolean> {

    MOBILISING_UNAIDED("schedule3MobilisingUnaided", UcActivityType.PHYSICAL_DISABILITIES),
    SITTING_POSITIONS("schedule3SittingPositions", UcActivityType.PHYSICAL_DISABILITIES),
    REACHING("schedule3Reaching", UcActivityType.PHYSICAL_DISABILITIES),
    PICKING_UP("schedule3PickingUp", UcActivityType.PHYSICAL_DISABILITIES),
    MANUAL_DEXTERITY("schedule3ManualDexterity", UcActivityType.PHYSICAL_DISABILITIES),
    MAKING_SELF_UNDERSTOOD("schedule3MakingSelfUnderstood", UcActivityType.PHYSICAL_DISABILITIES),
    COMMUNICATION("schedule3Communication", UcActivityType.PHYSICAL_DISABILITIES),
    LOSS_OF_CONTROL("schedule3LossOfControl", UcActivityType.PHYSICAL_DISABILITIES),
    LEARNING_TASKS("schedule3LearningTasks", UcActivityType.MENTAL_ASSESSMENT),
    AWARENESS_OF_HAZARDS("schedule3AwarenessOfHazards", UcActivityType.MENTAL_ASSESSMENT),
    PERSONAL_ACTION("schedule3PersonalAction", UcActivityType.MENTAL_ASSESSMENT),
    COPING_WITH_CHANGE("schedule3CopingWithChange", UcActivityType.MENTAL_ASSESSMENT),
    SOCIAL_ENGAGEMENT("schedule3SocialEngagement", UcActivityType.MENTAL_ASSESSMENT),
    APPROPRIATENESS_OF_BEHAVIOUR("schedule3AppropriatenessOfBehaviour", UcActivityType.MENTAL_ASSESSMENT),
    CONVEYING_FOOD_OR_DRINK("schedule3ConveyingFoodOrDrink", UcActivityType.PHYSICAL_DISABILITIES),
    CHEWING_OR_SWALLOWING("schedule3ChewingOrSwallowing", UcActivityType.PHYSICAL_DISABILITIES);

    final String key;
    final ActivityType activityType;

    UcSchedule3QuestionKey(String key, ActivityType activityType) {
        this.key = key;
        this.activityType = activityType;
    }

    public static UcSchedule3QuestionKey getByKey(String key) {
        for (UcSchedule3QuestionKey mapping : UcSchedule3QuestionKey.values()) {
            if (mapping.key.equals(key)) {
                return mapping;
            }
        }
        throw new IllegalArgumentException("Unknown ActivityQuestion for question key:" + key);
    }

    public String getKey() {
        return key;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public Function<SscsCaseData, Boolean> getAnswerExtractor() {
        return this::getBooleanAnswerToQuestion;
    }

    private Boolean getBooleanAnswerToQuestion(SscsCaseData caseData) {
        List<String> selected = caseData.getSchedule7Selections();
        if (selected != null && selected.contains(key)) {
            return true;
        } else {
            return null;
        }
    }
}
