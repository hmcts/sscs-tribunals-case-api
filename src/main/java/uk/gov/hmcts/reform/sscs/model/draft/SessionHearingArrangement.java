package uk.gov.hmcts.reform.sscs.model.draft;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Value;

@Value
@JsonInclude(value = JsonInclude.Include.NON_EMPTY)
public class SessionHearingArrangement {
    private Boolean requested;
    private String language;

    @JsonCreator
    public SessionHearingArrangement(Boolean requested, String language) {
        this.requested = requested;
        this.language = language;
    }

    @JsonCreator
    public SessionHearingArrangement(Boolean requested) {
        this.requested = requested;
        this.language = null;
    }
}
