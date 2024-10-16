package uk.gov.hmcts.reform.sscs.model.hmc.reference;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NextHearingVenueType {

    SOMEWHERE_ELSE("somewhereElse"),
    SAME_VENUE("sameVenue");

    @JsonValue
    private final String value;
}
