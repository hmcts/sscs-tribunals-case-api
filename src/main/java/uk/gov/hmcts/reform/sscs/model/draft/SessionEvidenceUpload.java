package uk.gov.hmcts.reform.sscs.model.draft;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Value;

@Value
public class SessionEvidenceUpload {
    @JsonProperty("items")
    private List<SessionEvidence> evidences;
}
