package uk.gov.hmcts.sscs.model.pdf;

import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;
import uk.gov.hmcts.sscs.model.ccd.CaseData;

@Value
@Builder
public class PdfWrapper {

    private SyaCaseWrapper syaCaseWrapper;

    private Long ccdCaseId;

    private CaseData caseData;
}
