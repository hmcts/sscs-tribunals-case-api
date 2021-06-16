package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityQuestion;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityType;

/**
 * Enum to encapsulate binding between an activity question key, and the actual question text.
 */
public class EsaActivityQuestion implements ActivityQuestion {

    final EsaQuestionKey<String> key;
    final String value;

    public EsaActivityQuestion(EsaQuestionKey key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key.getKey();
    }

    public String getValue() {
        return value;
    }

    @Override
    public ActivityType getActivityType() {
        return key.getActivityType();
    }

    public Function<SscsCaseData, String> getAnswerExtractor() {
        return key.getAnswerExtractor();
    }
}
