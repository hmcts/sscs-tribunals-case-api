package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityQuestion;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityType;

/**
 * Enum to encapsulate binding between an activity question key, and the corresponding getter method on SscsCaseData that yields that answer to that question.
 */
public enum EsaActivityQuestion implements ActivityQuestion {

    MOBILISING_UNAIDED("mobilisingUnaided", "Mobilising Unaided", EsaActivityType.PHYSICAL_DISABLITIES, SscsCaseData::getEsaWriteFinalDecisionMobilisingUnaidedQuestion);

    final String key;
    final String value;
    final ActivityType activityType;
    final Function<SscsCaseData, String> answerExtractor;

    EsaActivityQuestion(String key, String value, ActivityType activityType, Function<SscsCaseData, String> answerExtractor) {
        this.key = key;
        this.answerExtractor = answerExtractor;
        this.activityType = activityType;
        this.value = value;
    }

    public static EsaActivityQuestion getByKey(String key) {
        for (EsaActivityQuestion mapping : EsaActivityQuestion.values()) {
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

    @Override
    public ActivityType getActivityType() {
        return activityType;
    }

    public Function<SscsCaseData, String> getAnswerExtractor() {
        return answerExtractor;
    }
}
