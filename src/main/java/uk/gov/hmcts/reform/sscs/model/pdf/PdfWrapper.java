package uk.gov.hmcts.reform.sscs.model.pdf;

import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.domain.wrapper.SyaCaseWrapper;

@Value
@Builder
public class PdfWrapper {

    private SyaCaseWrapper syaCaseWrapper;

    private Long ccdCaseId;

    private SscsCaseData caseData;
}
