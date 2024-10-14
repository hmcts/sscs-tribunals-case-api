package uk.gov.hmcts.reform.sscs.model.hmc.reference;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PartyType {

    INDIVIDUAL("IND"),
    ORGANISATION("ORG");

    @JsonValue
    private final String partyLabel;
}
