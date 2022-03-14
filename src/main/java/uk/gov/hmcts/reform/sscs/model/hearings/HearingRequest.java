package uk.gov.hmcts.reform.sscs.model.hearings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingRoute;
import uk.gov.hmcts.reform.sscs.ccd.domain.HearingState;

@Data
@Builder(builderMethodName = "internalBuilder")
@RequiredArgsConstructor
public class HearingRequest implements SessionAwareRequest {

    @NonNull
    private final String ccdCaseId;

    private final HearingRoute hearingRoute;

    private final HearingState hearingState;

    public static HearingRequestBuilder builder(String ccdCaseId) {
        return internalBuilder().ccdCaseId(ccdCaseId);
    }

    @Override
    @JsonIgnore
    public String getSessionId() {
        return ccdCaseId;
    }
}
