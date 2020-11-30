package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsUcCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityType;

/**
 * Enum to encapsulate binding between an activity question key, and the corresponding getter method on SscsCaseData that yields that answer to that question.
 */
public enum UcActivityQuestionKey implements UcQuestionKey<String> {

    MOBILISING_UNAIDED("mobilisingUnaided", UcActivityType.PHYSICAL_DISABILITIES, SscsUcCaseData::getUcWriteFinalDecisionMobilisingUnaidedQuestion),
    STANDING_AND_SITTING("standingAndSitting", UcActivityType.PHYSICAL_DISABILITIES, SscsUcCaseData::getUcWriteFinalDecisionStandingAndSittingQuestion),
    REACHING("reaching", UcActivityType.PHYSICAL_DISABILITIES, SscsUcCaseData::getUcWriteFinalDecisionReachingQuestion),
    PICKING_UP("pickingUp", UcActivityType.PHYSICAL_DISABILITIES, SscsUcCaseData::getUcWriteFinalDecisionPickingUpQuestion),
    MANUAL_DEXTERITY("manualDexterity", UcActivityType.PHYSICAL_DISABILITIES, SscsUcCaseData::getUcWriteFinalDecisionManualDexterityQuestion),
    MAKING_SELF_UNDERSTOOD("makingSelfUnderstood", UcActivityType.PHYSICAL_DISABILITIES, SscsUcCaseData::getUcWriteFinalDecisionMakingSelfUnderstoodQuestion),
    COMMUNICATION("communication", UcActivityType.PHYSICAL_DISABILITIES, SscsUcCaseData::getUcWriteFinalDecisionCommunicationQuestion),
    NAVIGATION("navigation", UcActivityType.PHYSICAL_DISABILITIES, SscsUcCaseData::getUcWriteFinalDecisionNavigationQuestion),
    LOSS_OF_CONTROL("lossOfControl", UcActivityType.PHYSICAL_DISABILITIES, SscsUcCaseData::getUcWriteFinalDecisionLossOfControlQuestion),
    CONSCIOUSNESS("consciousness", UcActivityType.PHYSICAL_DISABILITIES, SscsUcCaseData::getUcWriteFinalDecisionConsciousnessQuestion),
    LEARNING_TASKS("learningTasks", UcActivityType.MENTAL_ASSESSMENT, SscsUcCaseData::getUcWriteFinalDecisionLearningTasksQuestion),
    AWARENESS_OF_HAZARDS("awarenessOfHazards", UcActivityType.MENTAL_ASSESSMENT, SscsUcCaseData::getUcWriteFinalDecisionAwarenessOfHazardsQuestion),
    PERSONAL_ACTION("personalAction", UcActivityType.MENTAL_ASSESSMENT, SscsUcCaseData::getUcWriteFinalDecisionPersonalActionQuestion),
    COPING_WITH_CHANGE("copingWithChange", UcActivityType.MENTAL_ASSESSMENT, SscsUcCaseData::getUcWriteFinalDecisionCopingWithChangeQuestion),
    GETTING_ABOUT("gettingAbout", UcActivityType.MENTAL_ASSESSMENT, SscsUcCaseData::getUcWriteFinalDecisionGettingAboutQuestion),
    SOCIAL_ENGAGEMENT("socialEngagement", UcActivityType.MENTAL_ASSESSMENT, SscsUcCaseData::getUcWriteFinalDecisionSocialEngagementQuestion),
    APPROPRIATENESS_OF_BEHAVIOUR("appropriatenessOfBehaviour", UcActivityType.MENTAL_ASSESSMENT, SscsUcCaseData::getUcWriteFinalDecisionAppropriatenessOfBehaviourQuestion);

    final String key;
    final ActivityType activityType;
    final Function<SscsCaseData, String> answerExtractor;

    UcActivityQuestionKey(String key, ActivityType activityType, Function<SscsUcCaseData, String> answerExtractor) {
        this.key = key;
        this.answerExtractor = c -> answerExtractor.apply(c.getSscsUcCaseData());
        this.activityType = activityType;
    }

    public static UcActivityQuestionKey getByKey(String key) {
        for (UcActivityQuestionKey mapping : UcActivityQuestionKey.values()) {
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

    public Function<SscsCaseData, String> getAnswerExtractor() {
        return answerExtractor;
    }
}
