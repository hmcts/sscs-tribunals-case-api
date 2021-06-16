package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.esa;

import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision.ActivityType;

public interface EsaQuestionKey<A> {

    String getKey();

    ActivityType getActivityType();

    Function<SscsCaseData, A> getAnswerExtractor();
}
