package uk.gov.hmcts.reform.sscs.model.draft;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Value;

@Value
public class SessionReasonForAppealing {
    @JsonProperty("items")
    private List<SessionReasonForAppealingItem> reasonForAppealingItems;
}
