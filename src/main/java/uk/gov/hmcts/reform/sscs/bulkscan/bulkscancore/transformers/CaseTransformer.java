package uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.transformers;

import java.util.Map;
import uk.gov.hmcts.reform.sscs.bulkscan.bulkscancore.domain.ExceptionRecord;
import uk.gov.hmcts.reform.sscs.domain.CaseResponse;
import uk.gov.hmcts.reform.sscs.idam.IdamTokens;

/**
 * Implementation of this interface will need to return CaseTransformationResponse.
 * If case transformation fails then errors field needs to be populated with appropriate message and field which failed transformation.
 */
public interface CaseTransformer {
    CaseResponse transformExceptionRecord(ExceptionRecord exceptionRecord, boolean combineWarnings);

    Map<String, Object> checkForMatches(Map<String, Object> sscsCaseData, IdamTokens token);

}
