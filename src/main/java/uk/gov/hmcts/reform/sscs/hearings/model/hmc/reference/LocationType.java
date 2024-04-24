package uk.gov.hmcts.reform.sscs.hearings.model.hmc.reference;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LocationType {

    COURT("court"),
    CLUSTER("cluster"),
    REGION("region");

    @JsonValue
    private final String locationLabel;
}
