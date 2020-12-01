package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import java.util.List;
import java.util.function.Function;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

/**
 * Enum to encapsulate an ActivityType, along with bindings to the methods on SscsCaseData that yield the specified award type for that ActivityType, and the answers to the multi-select list of
 * activities specified for the ActivityType.
 */
public interface ActivityType {

    Function<SscsCaseData, List<String>> getAnswersExtractor();

    String getName();
}
