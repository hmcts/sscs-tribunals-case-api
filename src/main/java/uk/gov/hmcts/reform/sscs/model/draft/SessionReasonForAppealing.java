package uk.gov.hmcts.reform.sscs.model.draft;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Value;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionReasonForAppealing {
    @JsonProperty("items")
    private List<SessionReasonForAppealingItem> reasonForAppealingItems;
}
