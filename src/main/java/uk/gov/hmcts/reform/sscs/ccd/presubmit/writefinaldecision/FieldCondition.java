package uk.gov.hmcts.reform.sscs.ccd.presubmit.writefinaldecision;

import java.util.Optional;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;

public interface FieldCondition {

    boolean isSatisified(SscsCaseData caseData);

    Optional<String> getOptionalErrorMessage(SscsCaseData sscsCaseData);

    Optional<String> getOptionalIsSatisfiedMessage(SscsCaseData caseData);

}
