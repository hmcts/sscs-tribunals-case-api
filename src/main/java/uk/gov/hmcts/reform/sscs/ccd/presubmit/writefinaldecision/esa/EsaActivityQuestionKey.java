package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityType;

/**
 * Enum to encapsulate binding between an activity question key, and the corresponding getter method on SscsCaseData that yields that answer to that question.
 */
public enum EsaActivityQuestionKey implements EsaQuestionKey<String> {

    MOBILISING_UNAIDED("mobilisingUnaided", EsaActivityType.PHYSICAL_DISABILITIES, SscsCaseData::getEsaWriteFinalDecisionMobilisingUnaidedQuestion),
    STANDING_AND_SITTING("standingAndSitting", EsaActivityType.PHYSICAL_DISABILITIES, SscsCaseData::getEsaWriteFinalDecisionStandingAndSittingQuestion),
    REACHING("reaching", EsaActivityType.PHYSICAL_DISABILITIES, SscsCaseData::getEsaWriteFinalDecisionReachingQuestion),
    PICKING_UP("pickingUp", EsaActivityType.PHYSICAL_DISABILITIES, SscsCaseData::getEsaWriteFinalDecisionPickingUpQuestion),
    MANUAL_DEXTERITY("manualDexterity", EsaActivityType.PHYSICAL_DISABILITIES, SscsCaseData::getEsaWriteFinalDecisionManualDexterityQuestion),
    MAKING_SELF_UNDERSTOOD("makingSelfUnderstood", EsaActivityType.PHYSICAL_DISABILITIES, SscsCaseData::getEsaWriteFinalDecisionMakingSelfUnderstoodQuestion),
    COMMUNICATION("communication", EsaActivityType.PHYSICAL_DISABILITIES, SscsCaseData::getEsaWriteFinalDecisionCommunicationQuestion),
    NAVIGATION("navigation", EsaActivityType.PHYSICAL_DISABILITIES, SscsCaseData::getEsaWriteFinalDecisionNavigationQuestion),
    LOSS_OF_CONTROL("lossOfControl", EsaActivityType.PHYSICAL_DISABILITIES, SscsCaseData::getEsaWriteFinalDecisionLossOfControlQuestion),
    CONSCIOUSNESS("consciousness", EsaActivityType.PHYSICAL_DISABILITIES, SscsCaseData::getEsaWriteFinalDecisionConsciousnessQuestion),
    LEARNING_TASKS("learningTasks", EsaActivityType.MENTAL_ASSESSMENT, SscsCaseData::getEsaWriteFinalDecisionLearningTasksQuestion),
    AWARENESS_OF_HAZARDS("awarenessOfHazards", EsaActivityType.MENTAL_ASSESSMENT, SscsCaseData::getEsaWriteFinalDecisionAwarenessOfHazardsQuestion),
    PERSONAL_ACTION("personalAction", EsaActivityType.MENTAL_ASSESSMENT, SscsCaseData::getEsaWriteFinalDecisionPersonalActionQuestion),
    COPING_WITH_CHANGE("copingWithChange", EsaActivityType.MENTAL_ASSESSMENT, SscsCaseData::getEsaWriteFinalDecisionCopingWithChangeQuestion),
    GETTING_ABOUT("gettingAbout", EsaActivityType.MENTAL_ASSESSMENT, SscsCaseData::getEsaWriteFinalDecisionGettingAboutQuestion),
    SOCIAL_ENGAGEMENT("socialEngagement", EsaActivityType.MENTAL_ASSESSMENT, SscsCaseData::getEsaWriteFinalDecisionSocialEngagementQuestion),
    APPROPRIATENESS_OF_BEHAVIOUR("appropriatenessOfBehaviour", EsaActivityType.MENTAL_ASSESSMENT, SscsCaseData::getEsaWriteFinalDecisionAppropriatenessOfBehaviourQuestion);

    final String key;
    final ActivityType activityType;
    final Function<SscsCaseData, String> answerExtractor;

    EsaActivityQuestionKey(String key, ActivityType activityType, Function<SscsCaseData, String> answerExtractor) {
        this.key = key;
        this.answerExtractor = answerExtractor;
        this.activityType = activityType;
    }

    public static EsaActivityQuestionKey getByKey(String key) {
        for (EsaActivityQuestionKey mapping : EsaActivityQuestionKey.values()) {
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
