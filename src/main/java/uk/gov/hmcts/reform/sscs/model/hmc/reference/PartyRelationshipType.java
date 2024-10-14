package uk.gov.hmcts.reform.sscs.model.hmc.reference;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PartyRelationshipType {

    SOLICITOR("11"),
    INTERPRETER("12");

    @JsonValue
    private final String relationshipTypeCode;

}
