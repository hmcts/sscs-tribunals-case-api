package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.uc;

import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityType;

public interface UcQuestionKey<A> {

    String getKey();

    ActivityType getActivityType();

    Function<SscsCaseData, A> getAnswerExtractor();
}
