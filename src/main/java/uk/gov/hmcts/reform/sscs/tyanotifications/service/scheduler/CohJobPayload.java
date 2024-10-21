package uk.gov.hmcts.reform.sscs.tyanotifications.service.scheduler;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class CohJobPayload {
    private long caseId;
    private String onlineHearingId;

    public CohJobPayload(@JsonProperty("case_id") long caseId, @JsonProperty("online_hearing_id") String onlineHearingId) {
        this.caseId = caseId;
        this.onlineHearingId = onlineHearingId;
    }
}
