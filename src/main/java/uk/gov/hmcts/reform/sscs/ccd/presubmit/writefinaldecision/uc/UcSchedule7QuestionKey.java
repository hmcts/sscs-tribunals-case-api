package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import java.util.List;
import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityType;

/**
 * Enum to encapsulate binding between an activity question key, and the corresponding getter method on SscsCaseData that yields that answer to that question.
 */
public enum UcSchedule7QuestionKey implements UcQuestionKey<Boolean> {

    MOBILISING_UNAIDED("schedule7MobilisingUnaided", UcActivityType.PHYSICAL_DISABILITIES),
    SITTING_POSITIONS("schedule7SittingPositions", UcActivityType.PHYSICAL_DISABILITIES),
    REACHING("schedule7Reaching", UcActivityType.PHYSICAL_DISABILITIES),
    PICKING_UP("schedule7PickingUp", UcActivityType.PHYSICAL_DISABILITIES),
    MANUAL_DEXTERITY("schedule7ManualDexterity", UcActivityType.PHYSICAL_DISABILITIES),
    MAKING_SELF_UNDERSTOOD("schedule7MakingSelfUnderstood", UcActivityType.PHYSICAL_DISABILITIES),
    COMMUNICATION("schedule7Communication", UcActivityType.PHYSICAL_DISABILITIES),
    LOSS_OF_CONTROL("schedule7LossOfControl", UcActivityType.PHYSICAL_DISABILITIES),
    LEARNING_TASKS("schedule7LearningTasks", UcActivityType.MENTAL_ASSESSMENT),
    AWARENESS_OF_HAZARDS("schedule7AwarenessOfHazards", UcActivityType.MENTAL_ASSESSMENT),
    PERSONAL_ACTION("schedule7PersonalAction", UcActivityType.MENTAL_ASSESSMENT),
    COPING_WITH_CHANGE("schedule7CopingWithChange", UcActivityType.MENTAL_ASSESSMENT),
    SOCIAL_ENGAGEMENT("schedule7SocialEngagement", UcActivityType.MENTAL_ASSESSMENT),
    APPROPRIATENESS_OF_BEHAVIOUR("schedule7AppropriatenessOfBehaviour", UcActivityType.MENTAL_ASSESSMENT),
    CONVEYING_FOOD_OR_DRINK("schedule7ConveyingFoodOrDrink", UcActivityType.PHYSICAL_DISABILITIES),
    CHEWING_OR_SWALLOWING("schedule7ChewingOrSwallowing", UcActivityType.PHYSICAL_DISABILITIES);

    final String key;
    final ActivityType activityType;

    UcSchedule7QuestionKey(String key, ActivityType activityType) {
        this.key = key;
        this.activityType = activityType;
    }

    public static UcSchedule7QuestionKey getByKey(String key) {
        for (UcSchedule7QuestionKey mapping : UcSchedule7QuestionKey.values()) {
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
        List<String> selected = caseData.getSscsUcCaseData().getSchedule7Selections();
        if (selected != null && selected.contains(key)) {
            return true;
        } else {
            return null;
        }
    }
}
