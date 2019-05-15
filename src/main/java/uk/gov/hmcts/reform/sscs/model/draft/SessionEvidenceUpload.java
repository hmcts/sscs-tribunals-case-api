package uk.gov.hmcts.reform.sscs.model.draft;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;
import uk.gov.hmcts.reform.sscs.ccd.domain.DocumentDetails;

import java.util.List;

@Value
public class SessionEvidenceUpload {
    @JsonProperty("items")
    private List<SessionEvidence> evidences;
}
