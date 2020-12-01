package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsEsaCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityType;

/**
 * Enum to encapsulate binding between an activity question key, and the corresponding getter method on SscsCaseData that yields that answer to that question.
 */
public enum EsaActivityQuestionKey implements EsaQuestionKey<String> {

    MOBILISING_UNAIDED("mobilisingUnaided", EsaActivityType.PHYSICAL_DISABILITIES, SscsEsaCaseData::getEsaWriteFinalDecisionMobilisingUnaidedQuestion),
    STANDING_AND_SITTING("standingAndSitting", EsaActivityType.PHYSICAL_DISABILITIES, SscsEsaCaseData::getEsaWriteFinalDecisionStandingAndSittingQuestion),
    REACHING("reaching", EsaActivityType.PHYSICAL_DISABILITIES, SscsEsaCaseData::getEsaWriteFinalDecisionReachingQuestion),
    PICKING_UP("pickingUp", EsaActivityType.PHYSICAL_DISABILITIES, SscsEsaCaseData::getEsaWriteFinalDecisionPickingUpQuestion),
    MANUAL_DEXTERITY("manualDexterity", EsaActivityType.PHYSICAL_DISABILITIES, SscsEsaCaseData::getEsaWriteFinalDecisionManualDexterityQuestion),
    MAKING_SELF_UNDERSTOOD("makingSelfUnderstood", EsaActivityType.PHYSICAL_DISABILITIES, SscsEsaCaseData::getEsaWriteFinalDecisionMakingSelfUnderstoodQuestion),
    COMMUNICATION("communication", EsaActivityType.PHYSICAL_DISABILITIES, SscsEsaCaseData::getEsaWriteFinalDecisionCommunicationQuestion),
    NAVIGATION("navigation", EsaActivityType.PHYSICAL_DISABILITIES, SscsEsaCaseData::getEsaWriteFinalDecisionNavigationQuestion),
    LOSS_OF_CONTROL("lossOfControl", EsaActivityType.PHYSICAL_DISABILITIES, SscsEsaCaseData::getEsaWriteFinalDecisionLossOfControlQuestion),
    CONSCIOUSNESS("consciousness", EsaActivityType.PHYSICAL_DISABILITIES, SscsEsaCaseData::getEsaWriteFinalDecisionConsciousnessQuestion),
    LEARNING_TASKS("learningTasks", EsaActivityType.MENTAL_ASSESSMENT, SscsEsaCaseData::getEsaWriteFinalDecisionLearningTasksQuestion),
    AWARENESS_OF_HAZARDS("awarenessOfHazards", EsaActivityType.MENTAL_ASSESSMENT, SscsEsaCaseData::getEsaWriteFinalDecisionAwarenessOfHazardsQuestion),
    PERSONAL_ACTION("personalAction", EsaActivityType.MENTAL_ASSESSMENT, SscsEsaCaseData::getEsaWriteFinalDecisionPersonalActionQuestion),
    COPING_WITH_CHANGE("copingWithChange", EsaActivityType.MENTAL_ASSESSMENT, SscsEsaCaseData::getEsaWriteFinalDecisionCopingWithChangeQuestion),
    GETTING_ABOUT("gettingAbout", EsaActivityType.MENTAL_ASSESSMENT, SscsEsaCaseData::getEsaWriteFinalDecisionGettingAboutQuestion),
    SOCIAL_ENGAGEMENT("socialEngagement", EsaActivityType.MENTAL_ASSESSMENT, SscsEsaCaseData::getEsaWriteFinalDecisionSocialEngagementQuestion),
    APPROPRIATENESS_OF_BEHAVIOUR("appropriatenessOfBehaviour", EsaActivityType.MENTAL_ASSESSMENT, SscsEsaCaseData::getEsaWriteFinalDecisionAppropriatenessOfBehaviourQuestion);

    final String key;
    final ActivityType activityType;
    final Function<SscsCaseData, String> answerExtractor;

    EsaActivityQuestionKey(String key, ActivityType activityType, Function<SscsEsaCaseData, String> answerExtractor) {
        this.key = key;
        this.answerExtractor = c -> answerExtractor.apply(c.getSscsEsaCaseData());
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
