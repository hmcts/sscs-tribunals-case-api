package uk.gov.hmcts.reform.sscs.model.hmc.reference;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum DayOfWeekUnavailabilityType {

    AM("AM"),
    PM("PM"),
    ALL_DAY("All Day");

    @JsonValue
    private final String label;
}
