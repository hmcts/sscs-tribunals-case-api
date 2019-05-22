package uk.gov.hmcts.reform.sscs.domain.wrapper;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class SyaReasonsForAppealing {

    private List<Reason> reasons;

    private String otherReasons;

    @JsonProperty("evidences")
    private List<SyaEvidence> evidences;

    private String evidenceDescription;

}
