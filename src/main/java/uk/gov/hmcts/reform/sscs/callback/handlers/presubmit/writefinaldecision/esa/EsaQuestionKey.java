package uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.writefinaldecision.esa;

import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.callback.handlers.presubmit.writefinaldecision.ActivityType;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public interface EsaQuestionKey<A> {

    String getKey();

    ActivityType getActivityType();

    Function<SscsCaseData, A> getAnswerExtractor();
}
