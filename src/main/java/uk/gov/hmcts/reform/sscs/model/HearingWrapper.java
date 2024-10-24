package uk.gov.hmcts.reform.sscs.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;
import uk.gov.hmcts.reform.sscs.ccd.domain.SscsCaseData;
import uk.gov.hmcts.reform.sscs.ccd.domain.State;
import uk.gov.hmcts.reform.sscs.reference.data.model.CancellationReason;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HearingWrapper {
    private SscsCaseData caseData;
    private State caseState;
    private String eventId;
    private String eventToken;
    private HearingState hearingState;
    private List<CancellationReason> cancellationReasons;
}
