package uk.gov.hmcts.sscs.model.pdf;

import lombok.Builder;
import lombok.Value;
import uk.gov.hmcts.sscs.domain.wrapper.SyaCaseWrapper;

@Value
@Builder
public class PdfWrapper {

    private SyaCaseWrapper syaCaseWrapper;

    private Long ccdCaseId;
}
