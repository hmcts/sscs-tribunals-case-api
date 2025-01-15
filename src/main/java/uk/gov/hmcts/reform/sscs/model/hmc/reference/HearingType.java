package uk.gov.hmcts.reform.sscs.model.hmc.reference;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.reform.sscs.ccd.domain.TypeOfHearing;

@Getter
@RequiredArgsConstructor
public enum HearingType {
    SUBSTANTIVE("BBA3-SUB", "Substantive", null),
    DIRECTION_HEARINGS("BBA3-DIR", "Direction Hearings", null),
    CHAMBERS_OUTCOME("BBA3-CHA", "Chambers Outcome", null);

    @JsonValue
    private final String hmcReference;
    private final String valueEn;
    private final String valueCy;

    public static HearingType getFromTypeOfHearing(TypeOfHearing typeOfHearing) {
        return switch (typeOfHearing) {
            case SUBSTANTIVE -> SUBSTANTIVE;
            case DIRECTION_HEARINGS -> DIRECTION_HEARINGS;
            case CHAMBERS_OUTCOME -> CHAMBERS_OUTCOME;
        };
    }
}
