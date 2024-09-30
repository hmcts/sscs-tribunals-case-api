package uk.gov.hmcts.reform.sscs.model;

import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.ADD_HEARING;
import static uk.gov.hmcts.reform.sscs.ccd.domain.EventType.UPDATE_HEARING_TYPE;

import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.Getter;
import uk.gov.hmcts.reform.sscs.ccd.domain.EventType;

@AllArgsConstructor
@Getter
public enum HearingEvent {
    ADJOURN_CREATE_HEARING(ADD_HEARING, "SSCS - Case sent to HMC", "Case with sent to HMC for Scheduling and Listing"),
    CREATE_HEARING(ADD_HEARING, "SSCS - Case sent to HMC", "Case with sent to HMC for Scheduling and Listing"),
    UPDATE_HEARING(UPDATE_HEARING_TYPE, "SSCS - Updates to case sent to HMC", "Case updates sent to HMC"),
    UPDATED_CASE(UPDATE_HEARING_TYPE, "SSCS - Updates to case sent to HMC", "Case updates sent to HMC");

    private final EventType eventType;
    private final String summary;
    private final String description;

    public static HearingEvent findByEventType(EventType eventType) {
        return Arrays.stream(values()).filter(o -> o.getEventType().equals(eventType)).findFirst().orElse(null);
    }
}
