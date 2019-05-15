package uk.gov.hmcts.reform.sscs.model.draft;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.util.List;

@Value
public class SessionDatesCantAttend {
    @JsonProperty("items")
    private List<SessionDate> datesCantAttend;
}
