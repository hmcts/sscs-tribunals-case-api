package uk.gov.hmcts.reform.sscs.model.draft;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.MrnDetails;

@Value
public class SessionMrnDate {
    @JsonProperty(value = "mrnDate")
    private SessionMrnDateDetails mrnDateDetails;

    public SessionMrnDate(MrnDetails mrnDetails) {
        this.mrnDateDetails = new SessionMrnDateDetails(mrnDetails);
    }
}
