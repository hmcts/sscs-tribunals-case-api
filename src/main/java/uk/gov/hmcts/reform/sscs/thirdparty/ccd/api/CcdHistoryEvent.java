package uk.gov.hmcts.reform.sscs.thirdparty.ccd.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;

public class CcdHistoryEvent {
    private final String id;

    public CcdHistoryEvent(@JsonProperty(value = "id")String id) {
        this.id = id;
    }

    public EventType getEventType() {
        return EventType.getEventTypeByCcdType(id);
    }
}
