package uk.gov.hmcts.reform.sscs.model.hearings;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class HearingRequest {

    private String ccdCaseId;

    private HearingRoute hearingRoute;

}
