package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import java.util.List;
import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityType;

/**
 * Enum to encapsulate binding between an activity question key, and the corresponding getter method on SscsCaseData that yields that answer to that question.
 */
public enum EsaSchedule3QuestionKey implements EsaQuestionKey<Boolean> {

    MOBILISING_UNAIDED("schedule3MobilisingUnaided", EsaActivityType.PHYSICAL_DISABILITIES),
    SITTING_POSITIONS("schedule3SittingPositions", EsaActivityType.PHYSICAL_DISABILITIES),
    REACHING("schedule3Reaching", EsaActivityType.PHYSICAL_DISABILITIES),
    PICKING_UP("schedule3PickingUp", EsaActivityType.PHYSICAL_DISABILITIES),
    MANUAL_DEXTERITY("schedule3ManualDexterity", EsaActivityType.PHYSICAL_DISABILITIES),
    MAKING_SELF_UNDERSTOOD("schedule3MakingSelfUnderstood", EsaActivityType.PHYSICAL_DISABILITIES),
    COMMUNICATION("schedule3Communication", EsaActivityType.PHYSICAL_DISABILITIES),
    LOSS_OF_CONTROL("schedule3LossOfControl", EsaActivityType.PHYSICAL_DISABILITIES),
    LEARNING_TASKS("schedule3LearningTasks", EsaActivityType.MENTAL_ASSESSMENT),
    AWARENESS_OF_HAZARDS("schedule3AwarenessOfHazards", EsaActivityType.MENTAL_ASSESSMENT),
    PERSONAL_ACTION("schedule3PersonalAction", EsaActivityType.MENTAL_ASSESSMENT),
    COPING_WITH_CHANGE("schedule3CopingWithChange", EsaActivityType.MENTAL_ASSESSMENT),
    SOCIAL_ENGAGEMENT("schedule3SocialEngagement", EsaActivityType.MENTAL_ASSESSMENT),
    APPROPRIATENESS_OF_BEHAVIOUR("schedule3AppropriatenessOfBehaviour", EsaActivityType.MENTAL_ASSESSMENT),
    CONVEYING_FOOD_OR_DRINK("schedule3ConveyingFoodOrDrink", EsaActivityType.PHYSICAL_DISABILITIES),
    CHEWING_OR_SWALLOWING("schedule3ChewingOrSwallowing", EsaActivityType.PHYSICAL_DISABILITIES);

    final String key;
    final ActivityType activityType;

    EsaSchedule3QuestionKey(String key, ActivityType activityType) {
        this.key = key;
        this.activityType = activityType;
    }

    public static EsaSchedule3QuestionKey getByKey(String key) {
        for (EsaSchedule3QuestionKey mapping : EsaSchedule3QuestionKey.values()) {
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
        List<String> selected = caseData.getSscsEsaCaseData().getSchedule3Selections();
        if (selected != null && selected.contains(key)) {
            return true;
        } else {
            return null;
        }
    }
}
