package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

/**
 * Enum to encapsulate binding between an activity question key, and the corresponding getter method on SscsCaseData that yields that answer to that question.
 */
public interface ActivityQuestion {

    String getKey();

    String getValue();

    ActivityType getActivityType();

    Function<SscsCaseData, String> getAnswerExtractor();
}
